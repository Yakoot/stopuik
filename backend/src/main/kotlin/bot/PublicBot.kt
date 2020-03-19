// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.spbelect.blacklist.SearchQuery
import org.spbelect.blacklist.handleSearchQuery
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.ApiConstants
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

fun main() {
  ServletContextHook().contextInitialized(null)
}

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

class PublicBot : TelegramLongPollingBot(DefaultBotOptions().apply {
  baseUrl = System.getenv("TG_BASE_URL") ?: ApiConstants.BASE_URL
}) {
  init {
    println("Created PublicBot")
  }
  override fun onUpdateReceived(update: Update) {
    update.callbackQuery?.let {
      AnswerCallbackQuery().apply {
        callbackQueryId = it.id
        text = "Фича в разработке!"
      }.also { execute(it) }
    }
    update.message?.let {msg ->
      try {
        update.handle().forEach { reply -> execute(reply) }
      } catch (ex: Exception) {
        ex.printStackTrace()
      }
    }
  }

  override fun getBotUsername() = System.getenv("TG_BOT_USERNAME") ?: "spbelect_public_bot"
  override fun getBotToken(): String = System.getenv("TG_BOT_TOKEN") ?: ""
}

fun help(): Response = Response("""
      Я умею искать по совпадениям в ФИО. Просто пошлите мне любую подстроку, например слово 'лето', убрав кавычки.
      Я умею искать по номеру комиссии. Просто пошлите мне номер УИК.
      """.trimIndent())

fun (Update).handle(): List<out BotApiMethod<Serializable>> = ChainBuilder(this.message).apply {
  onCommand("help") {
    reply(help().text, isMarkdown = false)
  }
  onCommand("start") {
    reply(help().text, isMarkdown = false)
  }
  onRegexp("""\d{1,4}""") {
    handleUikBlacklist(it.value.toInt()) { msg ->
      reply(msg)
    }
  }
  onRegexp("""(УИК)\s*(\d{1,4})""", setOf(RegexOption.IGNORE_CASE)) {
    val (uikType, uikNum) = it.destructured
    handleUikBlacklist(uikNum.toInt()) { msg ->
      reply(msg)
    }
  }
  onRegexp("""(ТИК)\s*(\d{1,2})""", setOf(RegexOption.IGNORE_CASE)) {
    //val (uikType, uikNum) = it.destructured
    reply("Поиск по ТИК пока не сделан\\.")
  }
  onRegexp("""(ИКМО)\s*(.+)""", setOf(RegexOption.IGNORE_CASE)) {
    //val (uikType, uikNum) = it.destructured
    reply("Поиск по ИКМО пока не сделан\\.")
  }
  onRegexp(""".+""", setOf(RegexOption.IGNORE_CASE)) {
    handlePersonBlacklist(it.value) { msg, jsons ->
      reply(msg, buttons = jsons.map { node ->
        BtnData(node["label"].textValue(), (node as ObjectNode).without<ObjectNode>("label").toString())
      }, maxCols = 1, isMarkdown = false)
    }
  }
}.handle()

private val LOGGER = LoggerFactory.getLogger("PublicBot")
