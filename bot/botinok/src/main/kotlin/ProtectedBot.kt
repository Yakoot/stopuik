package org.spbelect.blacklist.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.GlobalScope
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.io.Serializable
import java.sql.Date

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
          0 -> handleStart()
          1 -> handleUikNumberStart()
          2 -> handleDataInputStart()
          3 -> handleDataInputDateChoice(json)
          5 -> handleDocumentUploadDateChoice(json)
          6 -> handleDocumentUploadFinish()
          7 -> handleDocTypeChoice(json)
          8 -> handleSafeUsage(json)
          10 -> handleNumericKeyboard(json)
          else -> reply("Неизвестная команда")
        }
      }
      onDocument(whenState = WaitingState.WAITING_PHOTOS.ordinal) {
        handleDocumentUploadDone(it)
      }
      onCommand("start") {handleStart()}
      onRegexp("\\s*(\\d{1,4})", whenState = WaitingState.WAITING_UIK_NUM.ordinal) { handleUikNumberEnter(this.messageText.toIntOrNull()) }
      onRegexp("\\s*\\d+", whenState = WaitingState.WAITING_BOX_NUM.ordinal) {
        handleBoxNum(this.messageText.toIntOrNull())
      }
      onRegexp(".*", whenState = WaitingState.WAITING_SAFE_NUM.ordinal) {
        handleSafeNum(this.messageText)
      }
      onRegexp("\\s*\\d+", whenState = WaitingState.WAITING_VOTERS_CNT.ordinal) {
        handleVotersCnt(this.messageText.toIntOrNull())
      }
      onRegexp(".*", whenState = WaitingState.WAITING_NAME.ordinal) {
        handleName()
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

data class Observer(val tgId: Long, val uik: Int?, val firstName: String?, val lastName: String?,
                    val displayName: String?, val tgUsername: String?)

private fun getObserver(userId: Long): Observer? {
  return db {
    select(
        field("tg_id", Long::class.java),
        field("uik", Int::class.java),
        field("first_name", String::class.java),
        field("last_name", String::class.java),
        field("display_name", String::class.java),
        field("tg_username", String::class.java)
    ).from(
        table("Observer")
    ).where(
        field("tg_id").eq(userId)
    ).singleOrNull()?.let {
      Observer(userId, it.component2(), it.component3(), it.component4(), it.component5(), it.component6())
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
  } else if (observer.displayName == null) {
    handleNameStart()
  } else {
    reply(msg = "Ваша УИК: ${observer.uik}. Чего изволите?", isMarkdown = false, maxCols = 1, buttons = listOf(
        BtnData("Сменить номер УИК", jsonCallback {
          put("c", 1)
        }),
        BtnData("Сообщить данные о досрочке", jsonCallback {
          put("c", 2)
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
              field("last_name", String::class.java),
              field("tg_username", String::class.java)
          ).values(
              user.id.toLong(), user.firstName, user.lastName, user.userName
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
    dialogState(this@handleUikNumberStart.userId, WaitingState.WAITING_UIK_NUM.ordinal)
    reply("Введите номер УИК, на которой вы будете наблюдать. Только цифры.", isMarkdown = false)
    reply("УИК №:", isMarkdown = false)
  }
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

private fun ChainBuilder.handleNameStart() {
  db {
    dialogState(this@handleNameStart.userId, WaitingState.WAITING_NAME.ordinal)
    reply("Введите ваше имя и фамилию. Это нужно вашему координатору, чтобы быстро вас идентифицировать.", isMarkdown = false)
  }
}

private fun ChainBuilder.handleName() {
  db {
    transaction { _ ->
      update(table("Observer"))
          .set(field("display_name", String::class.java), this@handleName.messageText)
          .where(field("tg_id", Long::class.java).eq(this@handleName.userId))
          .execute()
      dialogState(this@handleName.userId, null, null)
      handleStart()
    }
  }

}

private fun ChainBuilder.handleDataInputStart() {
  db {
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
      val date = json["d"].asText()
      dialogState(this@handleDataInputDateChoice.userId, 2, date)
      reply("УИК №${observer.uik}. Дата: ${date}\nЭто голосование...",
          editMessageId = this@handleDataInputDateChoice.messageId, isMarkdown = false,
          buttons = listOf(
            BtnData("В помещении УИК", jsonCallback {
              put("c", 7)
              put("d", date)
              put("t", VotingType.INSIDE.ordinal)
            }),
            BtnData("ВНЕ помещения", jsonCallback {
              put("c", 7)
              put("d", date)
              put("t", VotingType.OUTSIDE.ordinal)
            })
          )
      )
    }
  }
}

enum class VotingType {
  INSIDE, OUTSIDE;
}
fun VotingType.displayText() = when (this) {
  VotingType.INSIDE -> "В помещении"
  VotingType.OUTSIDE -> "Вне помещения"
}

private fun ChainBuilder.handleDocTypeChoice(json: ObjectNode) {
  db {
    withObserver { observer ->
      val date = json["d"].asText()
      val type = VotingType.values()[json["t"].asInt()]
      reply("УИК №${observer.uik}. Дата: $date. ${type.displayText()}\nУ вас используются сейф-пакеты?",
          editMessageId = this@handleDocTypeChoice.messageId, isMarkdown = false, maxCols = 1, buttons = listOf(
        BtnData("Да, сейф-пакет есть, перекладываем бюллетени из ящика", jsonCallback {
          setAll<ObjectNode>(json)
          put("c", 8)
          put("y", true)
        }),
        BtnData("Нет сейф-пакетов, все бюллетени остаются в ящике", jsonCallback {
          setAll<ObjectNode>(json)
          put("c", 8)
          put("y", false)
        })
      ))
    }
  }
}

private fun ChainBuilder.handleSafeUsage(json: ObjectNode) {
  db {
    withObserver { observer ->
      if (json["y"].asBoolean()) {
        reply("Окей, вы перекладываете бюллетени из ящика в сейф-пакет.", isMarkdown = false)
      } else {
        reply("Окей, вы оставляете бюллетени в ящике и не считаете.", isMarkdown = false)
      }
      val date = json["d"].asText()
      val type = VotingType.values()[json["t"].asInt()]
      dialogState(observer.tgId, WaitingState.WAITING_BOX_NUM.ordinal, json.toString())
      reply("УИК №${observer.uik}. Дата: $date. ${type.displayText()}", isMarkdown = false)
      reply("Номер ящика:", isMarkdown = false)
    }
  }
}

private fun ChainBuilder.handleBoxNum(boxNum: Int?) {
  db {
    withObserver { observer ->
      this@handleBoxNum.dialogState?.let {state ->
        val json = jacksonObjectMapper().readTree(state.data) as ObjectNode
        json.put("b", boxNum)
        val date = json["d"].asText()
        val type = VotingType.values()[json["t"].asInt()]
        val hasSafe = json["y"].asBoolean()
        if (hasSafe) {
          dialogState(observer.tgId, WaitingState.WAITING_SAFE_NUM.ordinal, json.toString())
          reply("УИК №${observer.uik}. Дата: $date. ${type.displayText()}. Ящик №$boxNum.\nВы используете сейф-пакеты",
              isMarkdown = false)
          reply("Номер сейф-пакета:", isMarkdown = false)
        } else {
          dialogState(observer.tgId, WaitingState.WAITING_VOTERS_CNT.ordinal, json.toString())
          reply("""УИК №${observer.uik}. Дата: $date. ${type.displayText()}. Ящик №$boxNum.
|Вы НЕ используете сейф-пакеты. 
|Количеством проголосовавших будет разница между числом выданных бюллетеней и количеством испорченных.""".trimMargin(),
              isMarkdown = false)
          reply("Проголосовало:", isMarkdown = false)
        }
      }
    }
  }
}

private fun ChainBuilder.handleSafeNum(safeNum: String) {
  db {
    withObserver { observer ->
      this@handleSafeNum.dialogState?.let {state ->
        val json = jacksonObjectMapper().readTree(state.data) as ObjectNode
        json.put("s", safeNum)
        val date = json["d"].asText()
        val type = VotingType.values()[json["t"].asInt()]
        val boxNum = json["b"].asText()

        dialogState(observer.tgId, WaitingState.WAITING_VOTERS_CNT.ordinal, json.toString())
        reply("УИК №${observer.uik}. Дата: $date. ${type.displayText()}. Ящик №$boxNum. Сейф-пакет №$safeNum",
            isMarkdown = false)
        reply("Проголосовало:", isMarkdown = false)
      }
    }
  }
}

//reply("УИК №${observer.uik}. Дата: $date.", isMarkdown = false, maxCols = 5,
//buttons = buildKeypadButtons("${type.displayText()}: "))
//     dialogState(observer.tgId, WaitingState.WAITING_VOTERS_CNT.ordinal, json.asText())
//
//private fun ChainBuilder.handleHomeVotingDataInput(votersCount: Int?) {
//  handleDataInput("home_voters", votersCount) { db, observer, state ->
//    db.dialogState(observer.tgId, 3, state.data)
//    reply("УИК №${observer.uik}. Дата: ${state.data}?", isMarkdown = false)
//    reply("На участке:", isMarkdown = false, maxCols = 5,
//        buttons = buildKeypadButtons("На участке:"))
//  }
//}

private fun ChainBuilder.handleVotersCnt(votersCount: Int?) {
  db {
    withObserver { observer ->
      this@handleVotersCnt.dialogState?.let { state ->
        val json = jacksonObjectMapper().readTree(state.data) as ObjectNode
        if (json["y"].asBoolean()) {
          handleVotersCntWithSafe(votersCount, observer, json)
        } else {
          handleVotersCntNoSafe(votersCount, observer, json)
        }
      }
    }
  }
}

private fun ChainBuilder.handleVotersCntWithSafe(votersCount: Int?, observer: Observer, json: ObjectNode) {
  db {
    val date = json["d"].asText()
    val type = json["t"].asInt()
    val boxNum = json["b"].asInt()
    val safeNum = json["s"].asText()
    val q = insertInto(table("VotersCount")).columns(
        field("observer_id", Long::class.java),
        field("uik", Int::class.java),
        field("election_date", Date::class.java),
        field("safe_num", String::class.java)
    ).values(
        observer.tgId, observer.uik, Date.valueOf(date), safeNum
    ).onConflictDoNothing().execute()

    val votersCountId = select(field("id", Int::class.java)).from(table("VotersCount"))
        .where(
            field("observer_id", Long::class.java).eq(observer.tgId)
        ).and(
            field("uik", Int::class.java).eq(observer.uik)
        ).and(
            field("election_date", Date::class.java).eq(Date.valueOf(date))
        ).and(
            field("safe_num", String::class.java).eq(safeNum)
        ).firstOrNull()?.component1() ?: return@db

    update(table("VotersCount"))
        .set(field("voters_cnt", Int::class.java), votersCount)
        .set(field("box_num", Int::class.java), boxNum)
        .set(field("voting_type", Int::class.java), type)
        .where(
            field("id", Int::class.java).eq(votersCountId)
        )
        .execute()
    GlobalScope.launch(AIRTABLE_COROUTINE_DISPATCHER) {
      updateAirtable(votersCountId, observer, true)
    }
    reply("Записали. Фотографии актов есть?", isMarkdown = false, buttons = listOf(
        BtnData("Да, есть", jsonCallback {
          put("c", 5)
          put("id", votersCountId)
          put("d", date)
          put("y", true)
        }),
        BtnData("Нет фотографий", jsonCallback {
          put("c", 0)
        })
    ))
  }
}

private fun ChainBuilder.handleVotersCntNoSafe(votersCount: Int?, observer: Observer, json: ObjectNode) {
  db {
    val date = json["d"].asText()
    val type = json["t"].asInt()
    val boxNum = json["b"].asInt()
    insertInto(table("VotersCountNoSafe")).columns(
        field("observer_id", Long::class.java),
        field("uik", Int::class.java),
        field("election_date", Date::class.java),
        field("box_num", Int::class.java)
    ).values(
        observer.tgId, observer.uik, Date.valueOf(date), boxNum
    ).onConflictDoNothing().execute()

    val votersCountId = select(field("id", Int::class.java)).from(table("VotersCountNoSafe"))
        .where(
            field("observer_id", Long::class.java).eq(observer.tgId)
        ).and(
            field("uik", Int::class.java).eq(observer.uik)
        ).and(
            field("election_date", Date::class.java).eq(Date.valueOf(date))
        ).and(
            field("box_num", Int::class.java).eq(boxNum)
        ).firstOrNull()?.component1() ?: return@db

    update(table("VotersCountNoSafe"))
        .set(field("voters_cnt", Int::class.java), votersCount)
        .set(field("voting_type", Int::class.java), type)
        .where(
            field("id", Int::class.java).eq(votersCountId)
        )
        .execute()
    GlobalScope.launch(AIRTABLE_COROUTINE_DISPATCHER) {
      updateAirtable(votersCountId, observer, false)
    }
    reply("Записали. Фотографии актов есть?", isMarkdown = false, buttons = listOf(
        BtnData("Да, есть", jsonCallback {
          put("c", 5)
          put("id", votersCountId)
          put("d", date)
          put("y", false)
        }),
        BtnData("Нет фотографий", jsonCallback {
          put("c", 0)
        })
    ))
  }
}

private fun ChainBuilder.handleDocumentUploadDateChoice(json: ObjectNode) {
  db {
    if (json["d"].asText() == "") {
      dialogState(this@handleDocumentUploadDateChoice.userId, null)
      handleStart()
    } else {
      dialogState(this@handleDocumentUploadDateChoice.userId, WaitingState.WAITING_PHOTOS.ordinal, json.toString())
      reply("Засылайте фотографии. Можно несколько. Альбом, по одной, как документы -- неважно. Текст будет проигнорирован.", isMarkdown = false)
    }
  }
}

private fun ChainBuilder.handleDocumentUploadDone(docs: DocumentList) {
  withObserver { observer ->
    this.dialogState?.let {state ->
      val json = jacksonObjectMapper().readTree(state.data) as ObjectNode
      val recordId = json["id"].asInt()
      val hasSafe = json["y"].asBoolean()
      db {
        docs.docs.last().let { d ->
          if (hasSafe) {
            insertInto(table("VotersCountDocument")).columns(
                field("voters_count_id", Int::class.java),
                field("doc_id", String::class.java)
            ).values(
                recordId, d.docId
            ).execute()
          } else {
            insertInto(table("VotersCountDocument")).columns(
                field("voters_count_no_safe_id", Int::class.java),
                field("doc_id", String::class.java)
            ).values(
                recordId, d.docId
            ).execute()

          }
        }
      }

      GlobalScope.launch(AIRTABLE_COROUTINE_DISPATCHER) {
        updateAirtable(recordId, observer, hasSafe)
      }
      reply("Спасибо, записали.", isMarkdown = false)
      handleStart()
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
      reply("$prefix$currentValue$key", editMessageId = this.messageId,
          isMarkdown = false, maxCols = 5, buttons = buildKeypadButtons(prefix))
    }
    key == 10 -> {
      val newValue = if (currentValue.isEmpty()) "" else currentValue.substring(0, currentValue.lastIndex)
      reply("$prefix$newValue",
          editMessageId = this.messageId,
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
    WaitingState.WAITING_UIK_NUM.ordinal -> handleUikNumberEnter(value)
    WaitingState.WAITING_BOX_NUM.ordinal -> handleBoxNum(value)
    //WaitingState.WAITING_SAFE_NUM.ordinal -> handleSafeNum(value)
    WaitingState.WAITING_VOTERS_CNT.ordinal -> handleVotersCnt(value)
//    2 -> handleHomeVotingDataInput(value)
//    3 -> handleUikVotingDataInput(value)
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
    BtnData("ОТМЕНИТЬ", jsonCallback {
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

private fun <T> ChainBuilder.withObserver(code: (Observer) -> T) {
  val observer = getObserver(this.userId)
  if (observer == null) {
    handleUnknownUser()
  } else {
    code(observer)
  }
}

enum class WaitingState {
  WAITING_UIK_NUM, WAITING_NAME, WAITING_VOTERS_CNT, WAITING_PHOTOS, WAITING_BOX_NUM, WAITING_SAFE_NUM;
}
