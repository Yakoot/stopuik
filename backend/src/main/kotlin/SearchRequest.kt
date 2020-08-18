// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jooq.Record6
import org.jooq.SQLDialect
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL.*
import org.spbelect.blacklist.shared.dataSource

data class UikMemberStatus(
    var uik: Int,
    var tik: Int? = null,
    var year: Int,
    var uik_status: String
)

data class Crime(
    var description: String,
    var links: List<UikCrimeLink> = mutableListOf(),
    var uik: Int = 0
)

data class SearchQuery(
    var uik: Int = 0,
    var tik: String = "",
    var ikmo: String = "",
    var year: String = "",
    var report: String = "",
    var name: String = ""
) {
  val isBlank: Boolean get() {
    return listOf(year, ikmo, tik, report, name).all { it.isBlank() } && uik == 0
  }
}

data class SearchResult(
    var name: String = "",
    var id: Int = 0,
    var violations: MutableMap<String, List<Crime>> = mutableMapOf(),
    var status: MutableList<UikMemberStatus> = mutableListOf()
)

data class SearchResponse(
    var data: MutableList<SearchResult> = mutableListOf()
)

enum class StatusEnum(val label: String) {
  NOBODY("никто"), HEAD("председатель"), DEPUTY_HEAD("зам. председателя"), SECRETARY("секретарь"), VOTER("ПРГ")
}

typealias SearchRecord = Record6<Int, Int, Int, String, Int, Int>
fun (SearchRecord).status() = this.value5()
fun (SearchRecord).year() = this.value1()
fun (SearchRecord).name() = this.value4()
fun (SearchRecord).uik() = this.value2()

fun <T> prepareSearchQuery(searchQuery: SearchQuery, code: (SelectConditionStep<SearchRecord>) -> T): T {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    var q = ctx.select(
            field("year", Int::class.java),
            field("uik", Int::class.java),
            field("uik_member", Int::class.java),
            field("fio", String::class.java),
            field("uik_status", Int::class.java).`as`("status"),
            field("tik_id", Int::class.java))
        .from(table("UikMemberCrimeView"))
        .where(trueCondition())
    if (searchQuery.report.isNotBlank()) {
      q = q.and(field("crime_title").eq(searchQuery.report))
    }
    if (searchQuery.name.isNotBlank()) {
      q = q.and(field("fio").containsIgnoreCase(searchQuery.name))
    }
    searchQuery.year.toIntOrNull()?.let { q = q.and(field("year").eq(it)) }

    println(searchQuery)
    val uik = searchQuery.uik
    if (uik != 0) {
      q = q.and(field("uik").eq(uik))
      searchQuery.tik.toIntOrNull()?.let { tik ->
        q = q.and(field("tik_id").eq(0 - tik))
      }
      if (searchQuery.ikmo.isNotBlank()) {
        q = q.and(field("ikmo_name").eq(searchQuery.ikmo))
      }
    } else {
      val tik = searchQuery.tik.toIntOrNull()
      if (tik != null) {
        q = q.and(
            field("uik").eq(0 - tik).or(field("tik_id").eq(0 - tik))
        )
      }
      val ikmo = searchQuery.ikmo
      if (ikmo.isNotBlank()) {
        getIkmoId(ikmo)?.let { ikmoId ->
          q = q.and(
              field("uik").eq(ikmoId).or(field("ikmo_name").eq(ikmo))
          )
        }
      }
    }

    return code(q)
  }
}

fun handleSearchQuery(searchQuery: SearchQuery): SearchResponse {
  println(searchQuery)
  if (searchQuery.isBlank) {
    return SearchResponse()
  }
  return prepareSearchQuery(searchQuery) {q ->
    val resp = SearchResponse()
    var record = SearchResult()
    q.orderBy(
        field("fio"),
        field("year"),
        field("uik"),
        field("status"),
        field("uik_member")
    ).distinct().forEach {
      if (record.name != it["fio"]) {
        record = SearchResult(it["fio"].toString(), it["uik_member"].toString().toInt())
        resp.data.add(record)
      }
      record.status.add(UikMemberStatus(
          uik = it["uik"].toString().toInt(),
          tik = it["tik_id"]?.toString()?.toInt(),
          year = it["year"].toString().toInt(),
          uik_status = StatusEnum.values()[it.status()].label
      ))
    }
    resp
  }
}

fun handleDetailsQuery(query: UikCrimeQuery): UikCrimeResponse {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    // select year, uik, crime_id, crime_title, json_build_object('link_description', link_title, 'link', link_url)
    // from uikmembercrimeview u join uik_crime_links l on crime_id = l.uik_crime_id
    // where fio like 'Летовитез%'
    val q = ctx.select(
            field("year"),
            field("uik"),
            field("crime_id"),
            field("crime_title"),
            field("json_build_object('link_description', link_title, 'link', link_url) as crime_links"))
        .from(table("UikMemberCrimeView").join("uik_crime_links").on("crime_id = uik_crime_id"))
        .where(field("uik_member").eq(query.uik_member_id))
    val q1 = ctx.with("t").`as`(q).select(
        field("year"),
        field("uik"),
        field("crime_id"),
        field("crime_title"),
        field("json_agg(crime_links)::TEXT").`as`("links")
    ).from(table("T")).groupBy(
        field("year"),
        field("uik"),
        field("crime_id"),
        field("crime_title")
    ).orderBy(
        field("year"),
        field("crime_id")
    )

    val year2crimes = mutableMapOf<String, MutableList<Crime>>()
    q1.forEach {
      val crimes = year2crimes.getOrPut(it["year"].toString()) { mutableListOf() }
      val jsonNode: JsonNode = jacksonObjectMapper().readTree(it["links"].toString())

      if (jsonNode is ArrayNode) {
        val links = jsonNode.map { UikCrimeLink(
            link_description = it["link_description"].asText().trim('"'),
            link = it["link"].asText().trim('"')
        )}.toList()
        crimes.add(Crime(it["crime_title"].toString(), links, it["uik"].toString().toInt()))
      } else {
        println(jsonNode)
      }
    }
    return UikCrimeResponse(year2crimes)
  }
}

fun handleFullTextSearchQuery(query: String): FullTextResponse {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    val year2rawHistory = sortedMapOf<Int, MutableList<String>>()

    // First we run structured search over the parsed uik members data
    val q = ctx.select(
        field("year", Int::class.java),
        field("uik", Int::class.java),
        field("uik_member", Int::class.java),
        field("fio", String::class.java),
        field("uik_status", Int::class.java).`as`("status")
    ).from(table("uik_member").join(table("uik_history")).on("id = uik_member"))
        .where(field("fio").containsIgnoreCase(query))

    q.map { row ->
      """${row["fio"]} ${StatusEnum.values()[row["status"].toString().toInt()].label} УИК ${row["uik"]} в ${row["year"]}г."""
    }.also {
      if (it.isNotEmpty()) {
        year2rawHistory[2019] = it.toMutableList()
      }
    }


    // Then we run search over "raw" uik members data which was imported from
    // Yakov Sirotkin's github: https://github.com/YakovSirotkin/spbuik
    ctx.select(
        field("year", Int::class.java),
        field("raw_record", String::class.java)
    ).from(table("raw_history")).where(
        field("raw_record").containsIgnoreCase(query)
    ).orderBy(
        field("year").desc(), field("raw_record")
    ).forEach { row ->
        year2rawHistory.getOrPut(row["year"].toString().toInt()) { mutableListOf() }.also {
          it.add(row["raw_record"]?.toString() ?: "")
        }
    }

    val resultRows = mutableListOf<String>()
    year2rawHistory.toSortedMap(reverseOrder()).entries.forEach { entry ->
      resultRows.add("\nРезультаты поиска по составам УИК ${entry.key} года")
      resultRows.add("----------------------------------------------------")
      resultRows.addAll(entry.value)
    }


    return FullTextResponse(
        when {
          resultRows.isEmpty() -> listOf("Нет результатов")
          resultRows.size > 100 -> listOf("Найдено ${resultRows.size} результатов по всем годам. Уточните запрос, пожалуйста.")
          else -> resultRows
        }
    )
  }
}
