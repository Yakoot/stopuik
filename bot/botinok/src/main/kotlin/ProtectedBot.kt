package org.spbelect.blacklist.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.spbelect.blacklist.shared.db
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.ApiConstants
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.io.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Date
import java.util.concurrent.Executors

/**
 * @author dbarashev@bardsoftware.com
 */
fun main() {
  ApiContextInitializer.init()
  TelegramBotsApi().registerBot(ProtectedBot())
}

//@WebListener
//private class ProtectedBotServletContextHook : ServletContextListener {
//  override fun contextInitialized(sce: ServletContextEvent?) {
//    ApiContextInitializer.init()
//    try {
//      TelegramBotsApi().registerBot(ProtectedBot())
//    } catch (e: TelegramApiException) {
//      e.printStackTrace()
//    }
//  }
//
//  override fun contextDestroyed(sce: ServletContextEvent?) {
//  }
//}

private class ProtectedBot : TelegramLongPollingBot(DefaultBotOptions().apply {
  baseUrl = System.getenv("TG_BASE_URL") ?: ApiConstants.BASE_URL
}), MessageSender {
  init {
    println("Created Protected Bot")
  }
  override fun getBotUsername() = System.getenv("TG_BOT_USERNAME") ?: "spbelect_public_bot"
  override fun getBotToken(): String = System.getenv("TG_BOT_TOKEN") ?: ""
  override fun <T : BotApiMethod<Serializable>> send(msg: T) {
    execute(msg)
  }

  override fun onUpdateReceived(update: Update?) {
    if (update == null) {
      return
    }
    chain(update, this) {
      onCallback {json ->
        when (json["c"].asInt()) {
          1 -> handleUikNumberStart()
          2 -> handleDataInputStart()
          3 -> handleDataInputDateChoice(json)
          4 -> handleDocumentUploadStart()
          5 -> handleDocumentUploadDateChoice(json)
          6 -> handleDocumentUploadFinish()
          10 -> handleNumericKeyboard(json)
          else -> reply("Неизвестная команда")
        }
      }
      onDocument(whenState = 4) {
        handleDocumentUploadDone(it)
      }
      onCommand("start") {handleStart()}
      onRegexp("\\s*(\\d{1,4})", whenState = 1) { handleUikNumberEnter(this.messageText.toIntOrNull()) }
      onRegexp("\\s*\\d+", whenState = 2) {
        handleHomeVotingDataInput(this.messageText.toIntOrNull())
      }
      onRegexp("\\s*\\d+", whenState = 3) {
        handleUikVotingDataInput(this.messageText.toIntOrNull())
      }
      onRegexp(".*") {handleInvitation() || handleStart()}
    }
  }
}

private fun ChainBuilder.handleStart(): Boolean {
  val observer = getObserver(this.userId)
  if (observer == null) {
    this.handleUnknownUser()
  } else {
    this.handleObserver(observer)
  }
  return true
}

data class Observer(val tgId: Long, val uik: Int?, val firstName: String?, val lastName: String?)

private fun getObserver(userId: Long): Observer? {
  return db {
    select(
        field("tg_id", Long::class.java),
        field("uik", Int::class.java),
        field("first_name", String::class.java),
        field("last_name", String::class.java)
    ).from(
        table("Observer")
    ).where(
        field("tg_id").eq(userId)
    ).singleOrNull()?.let {
      Observer(userId, it.component2(), it.component3(), it.component4())
    }
  }
}

private fun ChainBuilder.handleUnknownUser() {
  reply("""
      |Привет, ${this.update.message.from.firstName} ${this.update.message.from.lastName}! 
      |Вас пока что нет в моих списках.
      |Пришлите мне свой одноразовый регистрационный пароль. 
      |Вам должен был выдать такой пароль ваш координатор.""".trimMargin(), isMarkdown = false)
}

private fun ChainBuilder.handleObserver(observer: Observer) {
  if (observer.uik == null) {
    handleUikNumberStart()
  } else {
    reply(msg = "Ваша УИК: ${observer.uik}. Чего изволите?", isMarkdown = false, maxCols = 1, buttons = listOf(
        BtnData("Сменить номер УИК", jsonCallback {
          put("c", 1)
        }),
        BtnData("Сообщить данные о досрочке", jsonCallback {
          put("c", 2)
        }),
        BtnData("Прислать фотографии документов", jsonCallback {
          put("c", 4)
        })
    ))
  }
}

private fun ChainBuilder.handleInvitation(): Boolean {
  return db {
    val invitation = this@handleInvitation.messageText.toLowerCase()
    val user = this@handleInvitation.update.message.from
    select(
        field("code", String::class.java), field("observer_id", Int::class.java)
    ).from("Invitation").where(
        field("code").eq(invitation)
    ).firstOrNull()?.let {
      println("Code $invitation seems to be valid.")

      if (it.component2() == null) {
        println("Code $invitation seems to be available. Registering user $user")
        transaction { _ ->
          insertInto(table("Observer")).columns(
              field("tg_id", Long::class.java),
              field("first_name", String::class.java),
              field("last_name", String::class.java)
          ).values(
              user.id.toLong(), user.firstName, user.lastName
          ).execute().also {
            println("INSERT executed: $it")
          }

          update(table("invitation"))
              .set(field("observer_id"), user.id.toLong())
              .where(field("code").eq(invitation))
              .execute().also {
                println("UPDATE executed: $it")
              }
          reply("Код принят. Вы зарегистрированы.", isMarkdown = false)
        }
        handleStart()
        return@db true
      } else {
        println("Code $invitation seems to be already used")
        reply("Этот код приглашения уже был использован.", isMarkdown = false)
      }
    }
    false
  }
}

private fun ChainBuilder.handleUikNumberStart() {
  db {
    dialogState(this@handleUikNumberStart.userId, 1)
  }
  reply("Введите номер УИК, на котором вы будете наблюдать. Только цифры.", isMarkdown = false)
  reply("УИК №:", isMarkdown = false, maxCols = 5,
      buttons = buildKeypadButtons("УИК №:"))
}

private fun ChainBuilder.handleUikNumberEnter(value: Int?) {
  db {
    transaction { _ ->
      update(table("Observer"))
          .set(field("uik", Int::class.java), value)
          .where(field("tg_id", Long::class.java).eq(this@handleUikNumberEnter.userId))
          .execute()
      dialogState(this@handleUikNumberEnter.userId, null, null)
      handleStart()
    }
  }
}

private fun ChainBuilder.handleDataInputStart() {
  db {
    reply("Подсказка: данные можно вводить произвольное количество раз. Записывается последнее введённое значение.", isMarkdown = false)
    reply("За какое число сведения?", isMarkdown = false, maxCols = 2, buttons = buildVotingDateButtons(3))
  }
}

private fun ChainBuilder.handleDataInputDateChoice(json: ObjectNode) {
  db {
    val observer = getObserver(this@handleDataInputDateChoice.userId)
    if (observer == null) {
      handleUnknownUser()
      return@db
    }
    if (json["d"].asText() == "") {
      dialogState(this@handleDataInputDateChoice.userId, null)
      handleStart()
    } else {
      dialogState(this@handleDataInputDateChoice.userId, 2, json["d"].asText())
      reply("УИК №${observer.uik}. Дата: ${json["d"].asText()}?", isMarkdown = false)
      reply("На дому:", isMarkdown = false, maxCols = 5,
          buttons = buildKeypadButtons("На дому:"))
    }
  }
}

private fun ChainBuilder.handleHomeVotingDataInput(votersCount: Int?) {
  handleDataInput("home_voters", votersCount) { db, observer, state ->
    db.dialogState(observer.tgId, 3, state.data)
    reply("УИК №${observer.uik}. Дата: ${state.data}?", isMarkdown = false)
    reply("На участке:", isMarkdown = false, maxCols = 5,
        buttons = buildKeypadButtons("На участке:"))
  }
}

private fun ChainBuilder.handleUikVotingDataInput(votersCount: Int?) {
  handleDataInput("uik_voters", votersCount) { db, observer, _ ->
    db.dialogState(observer.tgId, null)
    reply("Спасибо, записано", isMarkdown = false)
    handleStart()
  }
}

private fun ChainBuilder.handleDataInput(columnName: String, votersCount: Int?, closure: (DSLContext, Observer, DialogState) -> Unit) {
  val observer = getObserver(this.userId)
  if (observer == null) {
    handleUnknownUser()
  } else {
    this.fromUser?.getDialogState()?.let {
      db {
        insertInto(table("VotersCount")).columns(
            field("observer_id", Long::class.java),
            field("uik", Int::class.java),
            field("election_date", Date::class.java)
        ).values(observer.tgId, observer.uik, Date.valueOf(it.data)).onConflictDoNothing().execute()
        update(table("VotersCount"))
            .set(field(columnName, Int::class.java), votersCount)
            .where(
                field("observer_id", Long::class.java).eq(observer.tgId)
            ).and(
                field("election_date", Date::class.java).eq(Date.valueOf(it.data))
            ).and(
                field("uik", Int::class.java).eq(observer.uik)
            )
            .execute()
        closure(this, observer, it)
      }
      GlobalScope.launch(AIRTABLE_COROUTINE_DISPATCHER) {
        updateAirtable(observer, it.data ?: "")
      }
    }
  }
}

private fun ChainBuilder.handleDocumentUploadStart() {
  reply("За какое число фотографии?", isMarkdown = false, maxCols = 2, buttons = buildVotingDateButtons(5))
}

private fun ChainBuilder.handleDocumentUploadDateChoice(json: ObjectNode) {
  db {
    if (json["d"].asText() == "") {
      dialogState(this@handleDocumentUploadDateChoice.userId, null)
      handleStart()
    } else {
      dialogState(this@handleDocumentUploadDateChoice.userId, 4, json["d"].asText())
      reply("Засылайте фотографии. Можно несколько. Текст будет проигнорирован.", isMarkdown = false)
    }
  }
}

private fun ChainBuilder.handleDocumentUploadDone(docs: DocumentList) {
  val observer = getObserver(this.userId)
  if (observer == null) {
    handleUnknownUser()
  } else {
    this.update.message?.from?.getDialogState()?.let {
      db {
        insertInto(table("VotersCount")).columns(
            field("observer_id", Long::class.java),
            field("uik", Int::class.java),
            field("election_date", Date::class.java)
        ).values(
            observer.tgId, observer.uik, Date.valueOf(it.data)
        ).onConflictDoNothing().execute()

        docs.docs.last().let { d ->
          insertInto(table("VotersCountDocument")).columns(
              field("observer_id", Long::class.java),
              field("uik", Int::class.java),
              field("election_date", Date::class.java),
              field("doc_id", String::class.java)
          ).values(
              observer.tgId, observer.uik, Date.valueOf(it.data), d.docId
          ).execute()
        }
      }

      GlobalScope.launch(AIRTABLE_COROUTINE_DISPATCHER) {
        updateAirtable(observer, it.data ?: "")
      }
      reply("Спасибо, записали. Ещё фотографии есть?", isMarkdown = false, buttons = listOf(
          BtnData("Да, есть ещё", jsonCallback {
            put("c", 5)
            put("d", it.data)
          }),
          BtnData("Нет, пока всё", jsonCallback {
            put("c", 6)
          })
      ))
    }
  }
}

private fun ChainBuilder.handleDocumentUploadFinish() {
  val observer = getObserver(this.userId)
  if (observer == null) {
    handleUnknownUser()
  } else {
    this.fromUser?.getDialogState()?.let {
      db {
        dialogState(observer.tgId, null)
      }
    }
    handleStart()
  }
}

private fun ChainBuilder.handleNumericKeyboard(json: ObjectNode) {
  val prefix = json["p"].asText()
  val key = json["d"].asInt()
  val currentValue = this.update.callbackQuery.message.text.substring(prefix.length).toIntOrNull()?.toString() ?: ""
  when {
    key in 0..9 -> {
      reply("$prefix$currentValue$key", editMessageId = this.update.callbackQuery.message.messageId,
          isMarkdown = false, maxCols = 5, buttons = buildKeypadButtons(prefix))
    }
    key == 10 -> {
      val newValue = if (currentValue.isEmpty()) "" else currentValue.substring(0, currentValue.lastIndex)
      reply("$prefix$newValue",
          editMessageId = this.update.callbackQuery.message.messageId,
          isMarkdown = false, maxCols = 5, buttons = buildKeypadButtons(prefix)
      )
    }
    key == 11 -> {
      if (currentValue.isNotEmpty()) handleNumericInput(currentValue.toInt())
    }
    else -> {

    }
  }
}

private fun ChainBuilder.handleNumericInput(value: Int) {
  println("Numeric input: value=$value dialog state=${this.fromUser?.getDialogState()?.state}")
  when (this.fromUser?.getDialogState()?.state) {
    null -> {}
    1 -> handleUikNumberEnter(value)
    2 -> handleHomeVotingDataInput(value)
    3 -> handleUikVotingDataInput(value)
    else -> {

    }
  }
}

private fun DSLContext.dialogState(userId: Long, stateId: Int?, data: String? = null) {
  insertInto(table("DialogState"))
      .columns(
          field("observer_id", Long::class.java),
          field("state_id", Int::class.java),
          field("data", String::class.java)
      )
      .values(userId.toLong(), stateId, data)
      .onConflict(field("observer_id", Long::class.java)).doUpdate()
          .set(field("state_id", Int::class.java), stateId)
          .set(field("data", String::class.java), data)
      .execute()
}

private fun chain(update: Update, sender: MessageSender, handlers: (ChainBuilder.() -> Unit)) {
  ChainBuilder(update, sender).apply(handlers).handle().forEach { reply ->
    try {
      sender.send(reply)
    } catch (ex: TelegramApiRequestException) {
      ex.printStackTrace()
      when (reply) {
        is SendMessage -> {
          sender.send(SendMessage(reply.chatId, "Что-то сломалось при отправке ответа.") as BotApiMethod<Serializable>)
        }
        else -> println(reply)
      }
    }
  }
}

private fun ChainBuilder.buildVotingDateButtons(command: Int) = listOf(
    BtnData("25 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-25")
    }),
    BtnData("26 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-26")
    }),
    BtnData("27 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-27")
    }),
    BtnData("28 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-28")
    }),
    BtnData("29 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-29")
    }),
    BtnData("30 июня", jsonCallback {
      put("c", command)
      put("d", "2020-06-30")
    }),
    BtnData("ОЙ ВСЁ, ОТСТАНЬ", jsonCallback {
      put("c", command)
      put("d", "")
    })
)

private fun ChainBuilder.buildKeypadButtons(prefix: String) = listOf(
    BtnData("1", jsonCallback {
      put("c", 10)
      put("d", 1)
      put("p", prefix)
    }),
    BtnData("2", jsonCallback {
      put("c", 10)
      put("d", 2)
      put("p", prefix)
    }),
    BtnData("3", jsonCallback {
      put("c", 10)
      put("d", 3)
      put("p", prefix)
    }),
    BtnData("4", jsonCallback {
      put("c", 10)
      put("d", 4)
      put("p", prefix)
    }),
    BtnData("5", jsonCallback {
      put("c", 10)
      put("d", 5)
      put("p", prefix)
    }),
    BtnData("6", jsonCallback {
      put("c", 10)
      put("d", 6)
      put("p", prefix)
    }),
    BtnData("7", jsonCallback {
      put("c", 10)
      put("d", 7)
      put("p", prefix)
    }),
    BtnData("8", jsonCallback {
      put("c", 10)
      put("d", 8)
      put("p", prefix)
    }),
    BtnData("9", jsonCallback {
      put("c", 10)
      put("d", 9)
      put("p", prefix)
    }),
    BtnData("_  0  _", jsonCallback {
      put("c", 10)
      put("d", 0)
      put("p", prefix)
    }),
    BtnData("Delete", jsonCallback {
      put("c", 10)
      put("d", 10)
      put("p", prefix)
    }),
    BtnData("Enter", jsonCallback {
      put("c", 10)
      put("d", 11)
      put("p", prefix)
    })
)

private fun updateAirtable(observer: Observer, date: String) {
  val row = db {
    select(
        field("home_voters", Int::class.java),
        field("uik_voters", Int::class.java),
        field("airtable_record_id", String::class.java)
    ).from("VotersCount").where(
        field("observer_id", Long::class.java).eq(observer.tgId)
    ).and(
        field("uik", Int::class.java).eq(observer.uik)
    ).and(
        field("election_date", Date::class.java).eq(Date.valueOf(date))
    ).firstOrNull()
  } ?: return

  val jsonFields = jacksonObjectMapper().createObjectNode().apply {
    put("Наблюдатель", "${observer.firstName ?: ""} ${observer.lastName ?: ""}")
    put("Номер УИК", observer.uik)
    put("Дата", date)
    put("На дому", row.component1() ?: 0)
    put("На участке", row.component2() ?: 0)
  }
  db {
    select(
        field("doc_id", String::class.java)
    ).from("VotersCountDocument").where(
        field("observer_id", Long::class.java).eq(observer.tgId)
    ).and(
        field("uik", Int::class.java).eq(observer.uik)
    ).and(
        field("election_date", Date::class.java).eq(Date.valueOf(date))
    ).map { row ->
      jacksonObjectMapper().createObjectNode().apply {
        put("url", getDocumentUrl(row.component1()))
      }
    }.toList().also {
      if (it.isNotEmpty()) {
        jsonFields.put("Attachments", jacksonObjectMapper().createArrayNode().addAll(it))
      }
    }
  }
  val jsonBody = jacksonObjectMapper().createObjectNode().apply {
    put("fields", jsonFields)
  }

  println("""
    
    Sending to Airtable:
    --------------------
    $jsonBody
    
  """.trimIndent())
  if (row.component3() == null) {
    val req = HttpRequest.newBuilder()
        .uri(URI("https://api.airtable.com/v0/appx00G1cfwLC98Hs/Table%201"))
        .header("Authorization", "Bearer keyIuS44JJWT8ww9C")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
        .build()
    val textResponse = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body()
    val jsonResponse = jacksonObjectMapper().readTree(textResponse)
    if (!jsonResponse["id"].asText().isNullOrBlank()) {
      db {
        update(table("VotersCount"))
            .set(field("airtable_record_id", String::class.java), jsonResponse["id"].asText())
            .where(
                field("observer_id", Long::class.java).eq(observer.tgId)
            ).and(
                field("election_date", Date::class.java).eq(Date.valueOf(date))
            ).and(
                field("uik", Int::class.java).eq(observer.uik)
            )
            .execute()
      }
    }
  } else {
    val req = HttpRequest.newBuilder()
        .uri(URI("https://api.airtable.com/v0/appx00G1cfwLC98Hs/Table%201/${row.component3()}"))
        .header("Authorization", "Bearer keyIuS44JJWT8ww9C")
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
        .build()
    HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString())
  }
}

private fun getDocumentUrl(docId: String): String {
  val token = System.getenv("TG_BOT_TOKEN")
  val req = HttpRequest.newBuilder()
      .uri(URI("https://api.telegram.org/bot$token/getFile?file_id=$docId"))
      .GET()
      .build()
  val path = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body()?.let {
    jacksonObjectMapper().readTree(it).let { json ->
      json["result"].get("file_path").asText()
    }
  }
  return "https://api.telegram.org/file/bot$token/$path"
}
private val AIRTABLE_COROUTINE_DISPATCHER = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

// keyIuS44JJWT8ww9C
