// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.server.spi.config.Api
import com.google.api.server.spi.config.ApiMethod
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*

data class FilterData(
    var uik: MutableList<String> = mutableListOf(),
    var tik: MutableList<String> = mutableListOf(),
    var ikmo: MutableList<String> = mutableListOf(),
    var year: MutableList<String> = mutableListOf(),
    var report: MutableList<String> = mutableListOf()
)

data class SearchQuery(
  var uik: String = "",
  var tik: String = "",
  var ikmo: String = "",
  var year: String = "",
  var report: String = "",
  var name: String = ""
)

enum class StatusEnum(val label: String) {
  NOBODY("никто"), HEAD("председатель"), DEPUTY_HEAD("зам. председателя"), SECRETARY("секретарь"), VOTER("ПРГ")
}
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
data class SearchResult(
  var name: String = "",
  var id: Int = 0,
  var violations: MutableMap<String, List<Crime>> = mutableMapOf(),
  var status: MutableList<UikMemberStatus> = mutableListOf()
)

data class SearchResponse(
  var data: MutableList<SearchResult> = mutableListOf()
)

data class UikCrimeQuery(
    var uik_member_id: Int = 0
)

data class UikCrimeLink(
    var link_description: String,
    var link: String
)
data class UikCrimeResponse(
    var violations: MutableMap<String, MutableList<Crime>> = mutableMapOf()
)


@Api(name = "blacklist",
    version = "v1")
class Endpoint {

  @ApiMethod(name = "filters", httpMethod = "GET")
  fun filters(message: FilterData): FilterData {
    try {
      executeQuery("SELECT id, name FROM uik") { rs ->
        while (rs.next()) {
          val id = rs.getInt("id")
          when {
            id > 0 -> message.uik.add(id.toString())
            id < 0 && id > -100 -> message.tik.add((-id).toString())
            else -> rs.getString("name")?.let { message.ikmo.add(it) }
          }
        }
      }
      executeQuery("SELECT DISTINCT past_year AS year FROM Blacklist") {
        while (it.next()) {
          message.year.add(it.getString("year"))
        }
      }
      executeQuery("SELECT DISTINCT crime_title AS title FROM uik_crime") {
        while (it.next()) {
          val title = it.getString("title")
          if (!title.isNullOrBlank()) {
            message.report.add(title)
          }
        }
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
    return message
  }

  @ApiMethod(name = "search", httpMethod = "POST")
  fun search(searchQuery: SearchQuery): SearchResponse {
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
      searchQuery.uik.toIntOrNull()?.let {uik ->
        q = q.and(field("uik").eq(uik))
      }
      searchQuery.tik.toIntOrNull()?.let {tik ->
        q = q.and(field("tik_id").eq(0 - tik))
      }
      searchQuery.year.toIntOrNull()?.let { q = q.and(field("year").eq(it)) }
      if (searchQuery.ikmo.isNotBlank()) {
        q = q.and(field("ikmo_name").eq(searchQuery.ikmo))
      }
      if (searchQuery.report.isNotBlank()) {
        q = q.and(field("crime_title").eq(searchQuery.report))
      }
      if (searchQuery.name.isNotBlank()) {
        q = q.and(field("fio").containsIgnoreCase(searchQuery.name))
      }

      var record = SearchResult()
      q.orderBy(
          field("uik_member"),
          field("fio"),
          field("year"),
          field("uik"),
          field("status")
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

  @ApiMethod(name = "uik_crime", httpMethod = "POST", path = "uik_crime")
  fun getUikCrimes(query: UikCrimeQuery) : UikCrimeResponse {
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
          crimes.add(Crime(it["crime_title"].toString(), links))
        } else {
          println(jsonNode)
        }
      }
      return UikCrimeResponse(year2crimes)
    }

  }
}
