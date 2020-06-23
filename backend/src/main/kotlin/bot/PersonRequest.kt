// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist.bot

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*
import org.spbelect.blacklist.SearchQuery
import org.spbelect.blacklist.shared.dataSource
import org.spbelect.blacklist.handleSearchQuery

/**
 * @author dbarashev@bardsoftware.com
 */
fun handlePersonBlacklist(name: String, reply: (String, ArrayNode) -> Unit) {
  handleSearchQuery(SearchQuery(name = name)).let { resp ->
    val msgBuilder = StringBuilder()

    when (resp.data.size) {
      0 -> reply("Нет результатов", OBJECT_MAPPER.createArrayNode().also { json ->
        json.add(createFullTextSearch(name))
      })
      1 -> {
        val years = mutableListOf<Int>()
        val person = resp.data[0]
        msgBuilder.appendln("*${person.name.escapeMarkdown()}*")
        person.status.forEach { status ->
          years.add(status.year)
          msgBuilder.appendln("*${status.year}* ${status.uik_status.escapeMarkdown()} ${formatUikLabel(status.uik).escapeMarkdown()}")
        }
        reply(msgBuilder.toString(), OBJECT_MAPPER.createArrayNode().also {json ->
          years.forEach { year -> json.add(
              OBJECT_MAPPER.createObjectNode()
                  .put("year", year)
                  .put("label", "$year")
                  .put("person_id", person.id)
          ) }
          json.add(createFullTextSearch(name))
        })
      }
      in 2..10 -> {
        reply("Найдено несколько героев", OBJECT_MAPPER.createArrayNode().also { json->
          resp.data.forEach { person ->
            json.add(OBJECT_MAPPER.createObjectNode()
                .put("label", person.name)
                .put("person_id", person.id)
            )
          }
          json.add(createFullTextSearch(name))
        })
      }
      else -> {
        reply("Найдено ${resp.data.size} героев. Уточните запрос, пожалуйста".escapeMarkdown(),
            OBJECT_MAPPER.createArrayNode().also { it.add(createFullTextSearch(name)) })
      }
    }
  }
}

fun getPersonName(personId: Int): String {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    val row = ctx.select(
        field("fio", String::class.java)
    ).from(table("uik_member")).where(field("id").eq(personId)).fetchOne() ?: return ""
    return row["fio"].toString()
  }
}

private fun createFullTextSearch(name: String): JsonNode {
  val truncName = if (name.length >= 15) name.substring(0, 15) else name
  return OBJECT_MAPPER.createObjectNode()
      .put("command", "full_text")
      .put("label", "Искать $truncName по всем членам УИК")
      .put("query", truncName)
}
private val OBJECT_MAPPER = ObjectMapper()
