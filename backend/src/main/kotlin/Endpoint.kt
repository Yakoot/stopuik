// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.client.http.HttpStatusCodes
import com.google.api.server.spi.ServiceException
import com.google.api.server.spi.auth.common.User
import com.google.api.server.spi.config.Api
import com.google.api.server.spi.config.ApiMethod
import org.jooq.SQLDialect
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory

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

data class UikMembersQuery(
  var uik: Int = 0,
  var year: Int = 0
)

data class UikMembersResponseItem(
  var id: Int,
  var name: String,
  var status: Int
)

data class UikMembersResponse(
  var people: List<UikMembersResponseItem> = listOf()
)

data class AllUiksQuery(
  var year: Int = 0
)

enum class UikType {
  UIK, TIK, IKMO
}

data class AllUiksResponseItem(
    var name: String,
    var id: Int,
    var type: UikType,
    var managingUikId: Int
)

data class AllUiksResponse(
    var uiks: List<AllUiksResponseItem> = listOf()
)

data class CreateCrimeRequestLinkItem(
    var title: String = "",
    var url: String = ""
)

data class CreateCrimeRequest(
    var year: Int = 2019,
    var uik: Int = 0,
    var uikMembers: List<Int> = listOf(),
    var newUikMembers: List<String> = listOf(),
    var crimeType: String = "",
    var crimeLinks: List<CreateCrimeRequestLinkItem> = listOf()
)

data class CreateCrimeResponse(
    var crimeId: Int,
    var message: String
)

data class TimelineResponseItem(
  var year: Int = 0,
  var title: String = "",
  var date: String = "",
  var crimeCount: Int = 0,
  var uikMemberCount: Int = 0
)

data class TimelineResponse(
  var elections: List<TimelineResponseItem> = listOf()
)

@Api(name = "blacklist",
    version = "v1")
class Endpoint {

  @ApiMethod(name = "filters", httpMethod = "GET")
  fun filters(message: FilterData): FilterData {
    try {
      executeQuery("SELECT id, name FROM uik ORDER BY name") { rs ->
        while (rs.next()) {
          val id = rs.getInt("id")
          when {
            id >= 0 -> message.uik.add(id.toString())
            id < 0 && id > -100 -> message.tik.add((-id).toString())
            else -> rs.getString("name")?.let { message.ikmo.add(it) }
          }
        }
      }
      executeQuery("SELECT DISTINCT year FROM uik_member_history_crime") {
        while (it.next()) {
          message.year.add(it.getString("year"))
        }
      }
      executeQuery("SELECT DISTINCT crime_title AS title FROM uik_crime ORDER BY crime_title") {
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

  @ApiMethod(name = "uik_members", httpMethod = "POST", path = "uik_members")
  fun getUikMembers(query: UikMembersQuery): UikMembersResponse {
    using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      val members =
          ctx.select(
              field("fio"),
              field("uik_status"),
              field("id"))
              .from(table("uik_history").join("uik_member").on("uik_member = id"))
              .where(field("year").eq(query.year)).and(field("uik").eq(query.uik))
              .orderBy(field("uik_status"), field("fio"))
              .map {row ->
                UikMembersResponseItem(
                    id = row["id"].toString().toInt(),
                    name = row["fio"].toString(),
                    status = row["uik_status"].toString().toInt()
                )
              }.toList()
      return UikMembersResponse(people = members)
    }
  }

  @ApiMethod(name = "all_uiks", httpMethod = "POST", path = "all_uiks")
  fun getAllUiks(query: AllUiksQuery): AllUiksResponse {
    using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      return AllUiksResponse(
          uiks = ctx.select(
              field("id"),
              field("name"),
              field("type"),
              field("managing_id"))
              .from(table("alluiksview2019"))
              .map {row ->
                AllUiksResponseItem(
                    id = row["id"].toString().toInt(),
                    name = row["name"].toString(),
                    type = UikType.valueOf(row["type"].toString()),
                    managingUikId = row["managing_id"].toString().toInt()
                )
              }.toList()
      )
    }
  }

  @ApiMethod(name = "create_crime", httpMethod = "POST", path = "create_crime", authenticators = [AccessTokenAuthenticator::class])
  fun createCrime(query: CreateCrimeRequest, user: User): CreateCrimeResponse {

    val messages = mutableListOf<String>()
    try {
      val userId = using(dataSource, SQLDialect.POSTGRES).use { ctx ->
        val userRow = ctx.select(field("id"), field("permission"))
            .from(table("RegistryUser"))
            .where(field("uid").eq(user.id))
            .fetchOne()
        if (Permission.valueOf(userRow["permission"].toString().toUpperCase()) != Permission.WRITER) {
          throw ServiceException(HttpStatusCodes.STATUS_CODE_FORBIDDEN, "Вы не можете редактировать реестр, извините")
        }
        userRow["id"].toString().toLong()
      }

      using(dataSource, SQLDialect.POSTGRES).use { ctx ->
        val q = ctx.insertInto(table("uik_crime"), field("crime_title"))
            .values(query.crimeType)
            .returning(field("id"))
        val crimeId = q.fetchOne().getValue("id").toString().toInt()
        LOGGER.info("Create crime $crimeId")
        messages.add("Создано нарушение №$crimeId")

        // TODO: добавлять newUikMembers в таблицы
        val uikMembers = query.uikMembers
        uikMembers.forEach {
          ctx.insertInto(table("uik_member_history_crime"), field("crime_id"), field("year"), field("uik_member"))
              .values(crimeId, query.year, it).execute()
          LOGGER.info("Crime $crimeId was committed by UIK member $it in year ${query.year}")
        }
        messages.add("Проассоциировано с ${uikMembers.size} пациентами")

        query.crimeLinks.forEach {
          ctx.insertInto(table("uik_crime_links"), field("uik_crime_id"), field("link_title"), field("link_url"))
              .values(crimeId, it.title, it.url).execute()
          LOGGER.info("Associated link ${it.url} with crime $crimeId")
        }
        messages.add("Проассоциировано с ${query.crimeLinks.size} ссылками")

        ctx.insertInto(table("uik_crime_owner"), field("crime_id"), field("registry_user_id"))
            .values(crimeId, userId).execute();

        return CreateCrimeResponse(crimeId, messages.joinToString(separator = "\n"))
      }
    } catch (ex: DataAccessException) {
      LOGGER.error("Failed to execute createCrime", ex)
      LOGGER.info("Query was: $query")
      return CreateCrimeResponse(-1, ex.message ?: "")
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

  @ApiMethod(name = "timeline", httpMethod = "POST", path = "timeline")
  fun getTimeline(req: Any): TimelineResponse {
    using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      val elections =
          ctx.select(
              field("year"),
              field("title"),
              field("date"),
              field("crime_count"),
              field("uik_member_count"))
              .from(table("timeline"))
              .orderBy(field("year").desc())
              .map {row ->
                TimelineResponseItem(
                    year = row["year"].toString().toInt(),
                    title = row["title"].toString(),
                    date = row["date"].toString(),
                    crimeCount = row["crime_count"].toString().toInt(),
                    uikMemberCount = row["uik_member_count"].toString().toInt()
                )
              }.toList()
      return TimelineResponse(elections = elections)
    }
  }
}

private val LOGGER = LoggerFactory.getLogger("Endpoint")
