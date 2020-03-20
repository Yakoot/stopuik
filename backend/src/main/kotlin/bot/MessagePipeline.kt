package org.spbelect.blacklist.bot

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Preconditions
import com.google.common.escape.CharEscaper
import com.google.common.escape.Escapers
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable


data class Response(val text: String)
data class BtnData(val label: String, val callbackData: String)

typealias MessageHandler = (String) -> Unit
typealias MatchHandler = (MatchResult) -> Unit

open class ChainBuilder(private val messageText: String, private val replyChatId: Long) {
  private val handlers = mutableListOf<MessageHandler>()
  private var stopped = false
  private val replies = mutableListOf<BotApiMethod<Serializable>>()

  fun parseJson(code: (ObjectNode) -> Unit) {
    try {
      val jsonNode = OBJECT_MAPPER.readTree(this.messageText)
      if (jsonNode.isObject) {
        code(jsonNode as ObjectNode)
      }
    } catch (ex: JsonProcessingException) {
      ex.printStackTrace()
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

  fun onRegexp(pattern: String, options: Set<RegexOption> = setOf(), code: MatchHandler) {
    val regexp = pattern.toRegex(options)
    this.handlers += { msg ->
      regexp.matchEntire(msg.trim())?.let {
        code(it)
      }
    }
  }

  fun reply(msg: String, stop: Boolean = true, buttons: List<BtnData> = listOf(), maxCols: Int = Int.MAX_VALUE, isMarkdown: Boolean = true) {
    replies.add(SendMessage().apply {
      setChatId(this@ChainBuilder.replyChatId)
      enableMarkdownV2(isMarkdown)
      text = msg
      println(buttons)
      if (buttons.isNotEmpty()) {
        replyMarkup = InlineKeyboardMarkup(
            buttons.map { InlineKeyboardButton(it.label).also { btn -> btn.callbackData = it.callbackData } }.chunked(maxCols)
        )
      }
    } as BotApiMethod<Serializable>)
    this.stopped = this.stopped || stop
  }

  fun handle(): List<out BotApiMethod<Serializable>> {
    try {
      if (this.messageText.isNotBlank()) {
        for (h in handlers) {
          h(this.messageText)
          if (this.stopped) {
            break
          }
        }
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
    return replies
  }
}

private val escapedChars = charArrayOf('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')
private val ESCAPER = object : CharEscaper() {
  override fun escape(c: Char): CharArray {
    return if (escapedChars.contains(c)) charArrayOf('\\', c) else charArrayOf(c)
  }
}

fun (String).escapeMarkdown() = ESCAPER.escape(this)

private val OBJECT_MAPPER = ObjectMapper()
