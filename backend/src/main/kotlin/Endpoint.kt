// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.server.spi.auth.common.User
import com.google.api.server.spi.config.Api
import com.google.api.server.spi.config.ApiMethod
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory

data class FilterData(
    var uik: MutableList<String> = mutableListOf(),
    var tik: MutableList<String> = mutableListOf(),
    var ikmo: MutableList<String> = mutableListOf(),
    var year: MutableList<String> = mutableListOf(),
    var report: MutableList<String> = mutableListOf()
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
      executeQuery("SELECT value AS title FROM CrimeType ORDER BY value") {
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
  fun search(searchQuery: SearchQuery): SearchResponse = handleSearchQuery(searchQuery)

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
  fun createCrime(query: CreateCrimeRequest, user: User): CreateCrimeResponse  = handleCreateCrime(query, user)

  @ApiMethod(name = "uik_crime", httpMethod = "POST", path = "uik_crime")
  fun getUikCrimes(query: UikCrimeQuery) : UikCrimeResponse = handleDetailsQuery(query)

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

fun getIkmoId(ikmo: String): Int? {
  using(dataSource, SQLDialect.POSTGRES).use { ctx ->
    return ctx.select(
            field("id")
        )
        .from(table("alluiksview2019"))
        .where(field("name").eq(ikmo))
        .fetchOne()?.let {
          it["id"].toString().toInt()
        }
  }
}


private val LOGGER = LoggerFactory.getLogger("Endpoint")
