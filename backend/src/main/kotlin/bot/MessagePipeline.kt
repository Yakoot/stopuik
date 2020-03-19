package org.spbelect.blacklist.bot

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable


data class Response(val text: String)
data class BtnData(val label: String, val callbackData: String)

typealias MessageHandler = (String) -> Unit
typealias MatchHandler = (MatchResult) -> Unit

open class ChainBuilder(val message: Message) {
  private val handlers = mutableListOf<MessageHandler>()
  private var stopped = false
  private val replies = mutableListOf<BotApiMethod<Serializable>>()

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
      setChatId(message.chatId)
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
      message.text?.let { msg ->
        for (h in handlers) {
          h(msg)
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
