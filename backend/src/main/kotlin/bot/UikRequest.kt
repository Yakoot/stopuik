package org.spbelect.blacklist.bot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jooq.SQLDialect
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL.*
import org.spbelect.blacklist.*
import org.spbelect.blacklist.shared.dataSource

private val ikmoIdToName: Map<Int, String> by lazy {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    ctx.select(
            field("id"), field("name")
        )
        .from(table("alluiksview2019"))
        .where(field("id").lessThan(-100))
        .associate { row -> row["id"].toString().toInt() to row["name"].toString() }
  }
}

fun query2resultMap(sqlQuery: SelectConditionStep<SearchRecord>): Map<Int, List<SearchResult>> {
  val year2persons = sortedMapOf<Int, MutableList<SearchResult>>()
  sqlQuery.orderBy(field("year").desc(), field("status").asc()).distinct().forEach { it: SearchRecord ->
    val persons = year2persons.computeIfAbsent(it.year()) { mutableListOf() }
    persons.add(SearchResult(
        name = it.name(),
        status = mutableListOf(UikMemberStatus(
            uik = it.uik(),
            year = it.year(),
            uik_status = StatusEnum.values()[it.status()].label
        ))
    ))
  }
  return year2persons.toSortedMap(comparator = Comparator { year1, year2 -> year2 - year1 })
}

fun searchUik(uikId: Int): Map<Int, List<SearchResult>> {
  val searchQuery = SearchQuery(uik = uikId)
  return prepareSearchQuery(searchQuery) { q -> query2resultMap(q) }
}

fun buildUikBlacklistResponse(uikId: Int, year2persons: Map<Int, List<SearchResult>>): String {
  return if (year2persons.isEmpty()) {
    "В реестре нет сведений о нарушениях в ${formatUikLabel(uikId)}".escapeMarkdown()
  } else {
    val resultBuilder = StringBuilder("Нарушения в ${formatUikLabel(uikId)}\n".escapeMarkdown())
    year2persons.forEach { year, persons ->
      resultBuilder.appendln("*$year*")
      resultBuilder.appendln(persons.map { "${it.status[0].uik_status.escapeMarkdown()} ${formatUikLabel(it.status[0].uik).escapeMarkdown()} ${it.name.escapeMarkdown()}" }.joinToString(separator = "\n"))
    }
    resultBuilder.toString()
  }
}

fun handleUikBlacklist(uikId: Int, reply: (text: String) -> Unit) {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    ctx.select(field("id"), field("gas_url")).from("uik").where(field("id").eq(uikId))
        .firstOrNull()?.let { row ->
          if (row["gas_url"] != null) {
            reply("УИК №$uikId.\n".escapeMarkdown() + "[Официальные сведения](${row["gas_url"]})")
          }
        }
    }
  reply(buildUikBlacklistResponse(uikId, searchUik(uikId)))
}

fun handleTikBlacklist(tikNum: Int, reply: (String, ArrayNode) -> Unit) {
  reply(buildUikBlacklistResponse(tikNum,
      prepareSearchQuery(SearchQuery(tik = tikNum.toString())) {q -> query2resultMap(q)}),
      OBJECT_MAPPER.createArrayNode()
  )
}

fun handleIkmoBlacklist(ikmoName: String, reply: (String, ArrayNode) -> Unit) {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    val ikmos = ctx.select(
            field("id"), field("name")
        )
        .from(table("alluiksview2019"))
        .where(field("name").startsWithIgnoreCase(ikmoName)).and(field("type").eq("IKMO"))
        .map { row -> row["id"].toString().toInt() to row["name"].toString() }
        .toList()
    when (ikmos.size) {
      0 -> {
        reply("Поиск по ИКМО $ikmoName результатов не дал", OBJECT_MAPPER.createArrayNode())
      }
      1-> {
        val q = ctx.select(
                field("year", Int::class.java),
                field("uik", Int::class.java),
                field("uik_member", Int::class.java),
                field("fio", String::class.java),
                field("uik_status", Int::class.java).`as`("status"),
                field("tik_id", Int::class.java))
            .from(table("UikMemberCrimeView"))
            .where(field("ikmo_name").eq(ikmos[0].second)).or(field("uik").eq(ikmos[0].first))
        reply(buildUikBlacklistResponse(ikmos[0].first, query2resultMap(q)), OBJECT_MAPPER.createArrayNode())
      }
      else -> {
        reply("Уточните ИКМО", OBJECT_MAPPER.createArrayNode().also { json ->
          ikmos.forEach { (id, name) ->
            json.add(
                OBJECT_MAPPER.createObjectNode()
                    .put("label", "$name")
                    .put("ikmo_id", id)
            )
          }
        })
      }
    }
  }
}

fun formatUikLabel(uikId: Int): String {
  return when (uikId) {
    in 0..Int.MAX_VALUE -> "УИК $uikId"
    in -100..-1 -> "ТИК ${-uikId}"
    in -Int.MIN_VALUE..-100 -> "ИКМО ${ikmoIdToName[uikId]}"
    else -> "Неизвестная комиссия №$uikId"
  }
}

private val OBJECT_MAPPER = ObjectMapper()
