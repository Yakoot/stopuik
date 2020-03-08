// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import org.jooq.SQLDialect
import org.jooq.impl.DSL.*

data class UikMemberStatus(
    var uik: String,
    var tik: String,
    var year: String,
    var uik_status: String
)

data class Crime(
    var description: String,
    var links: List<UikCrimeLink> = mutableListOf()
)

data class SearchQuery(
    var uik: String = "",
    var tik: String = "",
    var ikmo: String = "",
    var year: String = "",
    var report: String = "",
    var name: String = ""
)

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

fun handleSearchQuery(searchQuery: SearchQuery): SearchResponse {
  println("search: $searchQuery")
  val resp = SearchResponse()
  if (listOf(searchQuery.uik, searchQuery.year, searchQuery.ikmo, searchQuery.tik, searchQuery.report, searchQuery.name).all { it.isBlank() }) {
    return resp
  }
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    var q = ctx.select(
            field("year"),
            field("uik"),
            field("uik_member"),
            field("fio"),
            field("uik_status").`as`("status"),
            field("tik_id"))
        .from(table("UikMemberCrimeView"))
        .where(trueCondition())
    if (searchQuery.report.isNotBlank()) {
      q = q.and(field("crime_title").eq(searchQuery.report))
    }
    if (searchQuery.name.isNotBlank()) {
      q = q.and(field("fio").containsIgnoreCase(searchQuery.name))
    }
    searchQuery.year.toIntOrNull()?.let { q = q.and(field("year").eq(it)) }

    val uik = searchQuery.uik.toIntOrNull()
    if (uik != null)  {
      q = q.and(field("uik").eq(uik))
      searchQuery.tik.toIntOrNull()?.let {tik ->
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
        getIkmoId(ikmo)?.let {ikmoId ->
          q = q.and(
              field("uik").eq(ikmoId).or(field("ikmo_name").eq(ikmo))
          )
        }
      }
    }

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
          it["uik"].toString(),
          it["tik_id"]?.toString()?.toIntOrNull()?.let { tik_id -> (-tik_id).toString() } ?: "",
          it["year"].toString(),
          StatusEnum.values()[it["status"].toString().toInt()].label
      ))
    }
    return resp
  }
}
