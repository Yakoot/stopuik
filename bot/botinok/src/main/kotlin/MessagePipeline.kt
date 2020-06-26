package org.spbelect.blacklist.bot

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.escape.CharEscaper
import org.jooq.impl.DSL.field
import org.spbelect.blacklist.shared.db
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.PhotoSize
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


data class Response(val text: String)
data class BtnData(val label: String, val callbackData: String = "")


typealias CallbackHandler = (ObjectNode) -> Unit
typealias MessageHandler = (String) -> Unit
typealias MatchHandler = (MatchResult) -> Unit

data class Document(val docId: String)
data class DocumentList(val docs : List<Document>)

typealias DocumentHandler = (DocumentList) -> Unit
interface MessageSender {
  fun <T: BotApiMethod<Serializable>> send(msg: T)
}

open class ChainBuilder(internal val update: Update, private val sendMessage: MessageSender) {
  val messageText = (update.message?.text ?: "").trim()
  val fromUser = update.message?.from ?: update.callbackQuery?.from
  val messageId = update.callbackQuery?.message?.messageId ?: update.message?.messageId

  val userId = (this.fromUser?.id ?: -1).toLong()
  val dialogState: DialogState? by lazy {
    this.fromUser?.getDialogState()
  }

  private var replyChatId = update.message?.chatId ?: -1

  private val callbackHandlers = mutableListOf<CallbackHandler>()
  private val documentHandlers = mutableListOf<DocumentHandler>()
  private val handlers = mutableListOf<MessageHandler>()
  private var stopped = false
  private val replies = mutableListOf<BotApiMethod<Serializable>>()

  fun parseJson(code: (ObjectNode) -> Unit) {
    try {
      val jsonNode = OBJECT_MAPPER.readTree(this.update.callbackQuery.data)
      if (jsonNode.isObject) {
        code(jsonNode as ObjectNode)
      } else {
        println("Malformed callback json: $jsonNode")
      }
    } catch (ex: JsonProcessingException) {
      ex.printStackTrace()
    }
  }

  fun onCallback(code: CallbackHandler) {
    this.callbackHandlers += code
  }

  fun onDocument(whenState: Int? = null, code: DocumentHandler) {
    this.documentHandlers += {docs ->
      if (whenState == null || this.dialogState?.state == whenState) {
        code(docs)
      }
    }
  }

  fun onCommand(command: String, code: MessageHandler) {
    this.handlers += { msg ->
      val slashedCommand = "/$command"
      if (msg.toLowerCase().startsWith(slashedCommand)) {
        code(msg.substring(slashedCommand.length).trim())
      } else {
        null
      }
    }
  }

  fun onRegexp(pattern: String, options: Set<RegexOption> = setOf(), whenState: Int? = null, code: MatchHandler) {
    val regexp = pattern.toRegex(options)
    this.handlers += { msg ->
      regexp.matchEntire(msg.trim())?.let {
        if (whenState == null || this.dialogState?.state == whenState) {
          code(it)
        } else {
          println("whenState=$whenState does not match the dialog state=${this.dialogState}")
        }

      }
    }
  }

  fun reply(msg: String, stop: Boolean = true,
            buttons: List<BtnData> = listOf(),
            maxCols: Int = Int.MAX_VALUE,
            isMarkdown: Boolean = true,
            editMessageId: Int? = null) {
    if (editMessageId == null) {
      replies.add(SendMessage().apply {
        setChatId(this@ChainBuilder.replyChatId)
        enableMarkdownV2(isMarkdown)
        text = msg
        if (buttons.isNotEmpty()) {
          replyMarkup = InlineKeyboardMarkup(
              buttons.map {
                InlineKeyboardButton(it.label).also { btn ->
                  if (it.callbackData.isNotBlank()) btn.callbackData = it.callbackData
                }
              }.chunked(maxCols)
          )
        }
      } as BotApiMethod<Serializable>)
      this.stopped = this.stopped || stop
    } else {
      replies.add(EditMessageText().apply {
        messageId = editMessageId
        setChatId(this@ChainBuilder.replyChatId)
        enableMarkdown(isMarkdown)
        text = msg
        if (buttons.isNotEmpty()) {
          replyMarkup = InlineKeyboardMarkup(
              buttons.map {
                InlineKeyboardButton(it.label).also { btn ->
                  if (it.callbackData.isNotBlank()) btn.callbackData = it.callbackData
                }
              }.chunked(maxCols)
          )
        }
      })
    }
  }

  fun handle(): List<out BotApiMethod<Serializable>> {
    try {
      when {
        !(this.update.message?.photo?.isNullOrEmpty() ?: true) -> {
          val docs = DocumentList(this.update.message.photo.map {
            Document(it.fileId)
          }.toList())

          for (h in documentHandlers) {
            h(docs)
            if (this.stopped) {
              break
            }
          }
        }
        this.update.message?.document != null -> {
          val docs = DocumentList(listOf(Document(this.update.message.document.fileId)))
          for (h in documentHandlers) {
            h(docs)
            if (this.stopped) {
              break
            }
          }
        }
        this.update.callbackQuery != null -> {
          this.replyChatId = this.update.callbackQuery.message.chatId
          this.update.callbackQuery.let { q ->
            AnswerCallbackQuery().apply {
              callbackQueryId = q.id
            }.also { sendMessage.send(it as BotApiMethod<Serializable>) }

            parseJson {json ->
              for (h in callbackHandlers) {
                h(json)
                if (this.stopped) {
                  break
                }
              }
            }
          }
        }

        this.messageText.isNotBlank() -> {
          for (h in handlers) {
            h(this.messageText)
            if (this.stopped) {
              break
            }
          }
        }

        else -> {}
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
    return replies
  }

  fun jsonCallback(builder: ObjectNode.() -> Unit) =
    OBJECT_MAPPER.createObjectNode().also(builder).toString()
}

private val escapedChars = charArrayOf('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')
private val ESCAPER = object : CharEscaper() {
  override fun escape(c: Char): CharArray {
    return if (escapedChars.contains(c)) charArrayOf('\\', c) else charArrayOf(c)
  }
}

fun (String).escapeMarkdown() = ESCAPER.escape(this)

fun (ArrayNode).item(builder: ObjectNode.() -> Unit) {
  this.add(OBJECT_MAPPER.createObjectNode().also(builder))
}

private val OBJECT_MAPPER = ObjectMapper()

data class DialogState(val state: Int, val data: String?)

fun User.getDialogState(): DialogState? {
  val userId = this.id
  return db {
    select(field("state_id", Int::class.java), field("data", String::class.java))
        .from("DialogState")
        .where(field("observer_id").eq(userId))
        .firstOrNull()?.let {
          if (it.component1() == null) null else DialogState(it.component1(), it.component2())
        }
  }
}
