package org.spbelect.blacklist.bot

import org.telegram.telegrambots.meta.api.objects.Message

/**
 * @author dbarashev@bardsoftware.com
 */
fun chain(code: ChainBuilder.() -> Unit): ChainBuilder {
  return try {
    ChainBuilder().also(code)
  } catch (e: Exception) {
    e.printStackTrace()
    FAILING_CHAIN_BUILDER
  }
}
val FAILING_CHAIN_BUILDER = ChainBuilder()

data class Response(val text: String)

typealias MessageHandler = (String) -> Response?
typealias MatchHandler = (MatchResult) -> Response?

class ChainBuilder {
  private val handlers = mutableListOf<MessageHandler>()
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

  fun handle(message: Message?): Response? {
    try {
      return message?.text?.let { msg ->
        for (h in handlers) {
          h(msg)?.let { return it }
        }
        return null
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      return null;
    }
  }
}
