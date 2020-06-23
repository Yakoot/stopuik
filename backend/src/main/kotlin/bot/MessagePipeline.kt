package org.spbelect.blacklist.bot

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.escape.CharEscaper
import org.jooq.impl.DSL.field
import org.spbelect.blacklist.shared.db
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable


data class Response(val text: String)
data class BtnData(val label: String, val callbackData: String = "")


typealias CallbackHandler = (ObjectNode) -> Unit
typealias MessageHandler = (String) -> Unit
typealias MatchHandler = (MatchResult) -> Unit

interface MessageSender {
  fun <T: BotApiMethod<Serializable>> send(msg: T)
}
open class ChainBuilder(internal val update: Update, private val sendMessage: MessageSender) {
  val messageText = (update.message?.text ?: "").trim()
  val userId = (update.message?.from?.id ?: update.callbackQuery?.from?.id ?: -1).toLong()

  private var replyChatId = update.message?.chatId ?: -1

  private val callbackHandlers = mutableListOf<CallbackHandler>()
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
        if (whenState == null || this.update?.message?.from?.getDialogState()?.state == whenState) {
          code(it)
        } else {
          println("whenState=$whenState does not match the dialog state=${this.update?.message?.from?.getDialogState()}")
        }

      }
    }
  }

  fun reply(msg: String, stop: Boolean = true, buttons: List<BtnData> = listOf(), maxCols: Int = Int.MAX_VALUE, isMarkdown: Boolean = true) {
    replies.add(SendMessage().apply {
      setChatId(this@ChainBuilder.replyChatId)
      enableMarkdownV2(isMarkdown)
      text = msg
      if (buttons.isNotEmpty()) {
        replyMarkup = InlineKeyboardMarkup(
            buttons.map {
              println("label=${it.label} size=${it.label.length} callback=${it.callbackData}")
              InlineKeyboardButton(it.label).also { btn ->
                if (it.callbackData.isNotBlank()) btn.callbackData = it.callbackData
              }
            }.chunked(maxCols)
        )
      }
    } as BotApiMethod<Serializable>)
    this.stopped = this.stopped || stop
  }

  fun handle(): List<out BotApiMethod<Serializable>> {
    try {
      when {
        this.update.callbackQuery != null -> {
          println(this.update)
          this.replyChatId = this.update.callbackQuery.message.chatId
          this.update.callbackQuery?.let { q ->
            AnswerCallbackQuery().apply {
              callbackQueryId = q.id
            }.also { sendMessage.send(it as BotApiMethod<Serializable>) }

            parseJson {json ->
              println(json)
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
        .firstOrNull()?.let { DialogState(it.component1(), it.component2()) }
  }
}
