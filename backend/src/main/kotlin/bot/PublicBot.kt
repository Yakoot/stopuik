package org.spbelect.blacklist.bot

import org.spbelect.blacklist.SearchQuery
import org.spbelect.blacklist.handleSearchQuery
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import kotlin.text.StringBuilder

@WebListener
class ServletContextHook : ServletContextListener {
  override fun contextInitialized(sce: ServletContextEvent?) {
    ApiContextInitializer.init()
    try {
      TelegramBotsApi().registerBot(PublicBot())
    } catch (e: TelegramApiException) {
      e.printStackTrace()
    }
  }

  override fun contextDestroyed(sce: ServletContextEvent?) {
  }
}

class PublicBot : TelegramLongPollingBot() {
  init {
    println("Created PublicBot")
  }
  override fun onUpdateReceived(update: Update) {
    update.message?.let {msg ->
      update.handle()?.let {resp ->
        try {
          execute(SendMessage().apply {
            setChatId(msg.chatId)
            text = resp.text
          })
        } catch (e: TelegramApiException) {
          e.printStackTrace()
        }
      }
    }
  }

  override fun getBotUsername() = "spbelect_public_bot"
  override fun getBotToken(): String = System.getenv("BOT_TOKEN") ?: ""
}

fun (Update).handle(): Response? = chain {
    onRegexp("""\d{1,4}""") {
      handleSearchQuery(SearchQuery(uik = it.value)).let {resp ->
        Response(resp.data.map { it.name }.joinToString("\n"))
      }
    }
    onRegexp("""(УИК)\s*(\d{1,4})""", setOf(RegexOption.IGNORE_CASE)) {
      val (uikType, uikNum) = it.destructured
      handleSearchQuery(SearchQuery(uik = uikNum)).let {resp ->
        Response(resp.data.map { it.name }.joinToString("\n"))
      }
    }
    onRegexp("""(ТИК)\s*(\d{1,2})""", setOf(RegexOption.IGNORE_CASE)) {
      val (uikType, uikNum) = it.destructured
      Response("Ищем комиссию ${uikType.toUpperCase()} ${uikNum}")
    }
    onRegexp(""".+""", setOf(RegexOption.IGNORE_CASE)) {
      handleSearchQuery(SearchQuery(name = it.value)).let {resp ->
        val resultBuilder = StringBuilder()
        resp.data.forEach { person ->
          resultBuilder.appendln(person.name)
          person.status.forEach { status ->
            resultBuilder.appendln("${status.year} ${status.uik_status} ${status.uik}")
          }
          resultBuilder.appendln("")
          person.violations.forEach { year, crime ->
            resultBuilder.appendln("$year: $crime")
          }
          resultBuilder.appendln("============\n")
        }
        Response(resultBuilder.toString())
      }
    }
  }.handle(this.message)
