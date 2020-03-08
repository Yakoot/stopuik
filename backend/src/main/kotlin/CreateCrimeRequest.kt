// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.google.api.client.http.HttpStatusCodes
import com.google.api.server.spi.ServiceException
import com.google.api.server.spi.auth.common.User
import org.jooq.SQLDialect
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory

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

fun handleCreateCrime(query: CreateCrimeRequest, user: User): CreateCrimeResponse {
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

    val crimeTypeId = using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      ctx.select(field("id"))
          .from(table("CrimeType"))
          .where(field("value").eq(query.crimeType))
          .fetchOne()?.getValue("id")?.toString()?.toIntOrNull()
          ?: throw ServiceException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "Ошибка при добавлении нового пациента")
    }

    using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      val q = ctx.insertInto(table("uik_crime"), field("type_id"))
          .values(crimeTypeId)
          .returning(field("id"))
      val crimeId = q.fetchOne().getValue("id").toString().toInt()
      LOGGER.info("Create crime $crimeId")
      messages.add("Создано нарушение №$crimeId")

      val uikMembers = query.uikMembers.toMutableList()

      query.newUikMembers.forEach {newFio ->
        val maxId = ctx.select(max(field("id")).`as`("id")).from(table("uik_member"))
            .fetchOne()?.getValue("id")?.toString()?.toInt()
            ?: throw ServiceException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "Ошибка при добавлении нового пациента")
        val newMemberId = maxId + 1
        ctx.insertInto(table("uik_member"), field("id"), field("fio"))
            .values(newMemberId, newFio).execute()
        ctx.insertInto(table("uik_history"), field("uik"), field("year"), field("uik_member"), field("uik_status"))
            .values(query.uik, query.year, newMemberId, StatusEnum.VOTER.ordinal)
        .execute()
        uikMembers.add(newMemberId)
      }

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
    ex.printStackTrace()
    LOGGER.error("Failed to execute createCrime", ex)
    LOGGER.info("Query was: $query")
    return CreateCrimeResponse(-1, ex.message ?: "")
  } catch (ex: Exception) {
    LOGGER.error("Failed to execute createCrime", ex)
    LOGGER.info("Query was: $query")
    throw ServiceException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, ex.message, ex)
  }
}

private val LOGGER = LoggerFactory.getLogger("Endpoint")
