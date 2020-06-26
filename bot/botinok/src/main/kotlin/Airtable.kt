package org.spbelect.blacklist.bot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.asCoroutineDispatcher
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.spbelect.blacklist.shared.db
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Date
import java.util.concurrent.Executors

/**
 * @author dbarashev@bardsoftware.com
 */
fun updateAirtable(postgresRecordId: Int, observer: Observer, hasSafe: Boolean) {
  val jsonFields = db {
    if (hasSafe) {
      select(
          field("observer_id", Long::class.java),
          field("uik", Int::class.java),
          field("election_date", Date::class.java),
          field("safe_num", String::class.java),
          field("voters_cnt", Int::class.java),
          field("box_num", Int::class.java),
          field("voting_type", Int::class.java),
          field("airtable_record_id", String::class.java)
      ).from("VotersCount").where(
          field("id", Int::class.java).eq(postgresRecordId)
      ).firstOrNull()?.let {row ->
        jacksonObjectMapper().createObjectNode().apply {
          put("Имя и фамилия", "${observer.displayName ?: ""}")
          put("Telegram", "${observer.tgUsername ?: ""}")
          put("telegram_user_id", observer.tgId)
          put("Номер УИК", row.component2())
          put("Дата", row.component3().toString())
          put("Акт", VotingType.values()[row.component7()].displayText())
          put("Номер ящика", row.component6())
          put("Номер сейф-пакета", row.component4())
          put("Число участников", row.component5())
          row.component8()?.let {
            put("airtable_id", it)
          }
        }
      }
    } else {
      select(
          field("observer_id", Long::class.java),
          field("uik", Int::class.java),
          field("election_date", Date::class.java),
          field("voters_cnt", Int::class.java),
          field("box_num", Int::class.java),
          field("voting_type", Int::class.java),
          field("airtable_record_id", String::class.java)
      ).from("VotersCountNoSafe").where(
          field("id", Int::class.java).eq(postgresRecordId)
      ).firstOrNull()?.let {row ->
        jacksonObjectMapper().createObjectNode().apply {
          put("Имя и фамилия", "${observer.displayName ?: ""}")
          put("Telegram", "${observer.tgUsername ?: ""}")
          put("telegram_user_id", observer.tgId)
          put("Номер УИК", row.component2())
          put("Дата", row.component3().toString())
          put("Акт", VotingType.values()[row.component6()].displayText())
          put("Номер ящика", row.component5())
          put("Число участников", row.component4())
          row.component7()?.let {
            put("airtable_id", it)
          }
        }
      }
    }
  } ?: return

  db {
    val docidQuery = if (hasSafe) {
        select(
          field("doc_id", String::class.java)
        ).from("VotersCountDocument").where(
          field("voters_count_id", Int::class.java).eq(postgresRecordId)
        )
    } else {
      select(
          field("doc_id", String::class.java)
      ).from("VotersCountDocument").where(
          field("voters_count_no_safe_id", Int::class.java).eq(postgresRecordId)
      )
    }
    docidQuery.map { row ->
      jacksonObjectMapper().createObjectNode().apply {
        put("url", getDocumentUrl(row.component1()))
      }
    }.toList().also {
      if (it.isNotEmpty()) {
        jsonFields.put("Фотография", jacksonObjectMapper().createArrayNode().addAll(it))
      }
    }
  }

  val airtableRecord = jsonFields["airtable_id"]?.asText()
  jsonFields.remove("airtable_id")

  val jsonBody = jacksonObjectMapper().createObjectNode().apply {
    put("fields", jsonFields)
  }

  val updateTable = if (hasSafe) "VotersCount" else "VotersCountNoSafe"
  val airtable = if (hasSafe) AIRTABLE_WITH_SAFES else AIRTABLE_NO_SAFE
  println("""
    
    Sending to Airtable: $airtable (record=$airtableRecord)
    --------------------
    $jsonBody
    
  """.trimIndent())
  if (airtableRecord == null) {
    val req = HttpRequest.newBuilder()
        .uri(URI(airtable))
        .header("Authorization", "Bearer $AIRTABLE_API_KEY")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
        .build()
    val resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString())
    if (resp.statusCode() == 200) {
      println(resp)
      val textResponse = resp.body()
      val jsonResponse = jacksonObjectMapper().readTree(textResponse)
      if (!jsonResponse["id"].asText().isNullOrBlank()) {
        db {
          update(DSL.table(updateTable))
              .set(field("airtable_record_id", String::class.java), jsonResponse["id"].asText())
              .where(
                  field("id", Int::class.java).eq(postgresRecordId)
              )
              .execute()
        }
      }
    } else {
      println("Response=${resp.statusCode()} error=${resp.toString()}")
    }
  } else {
    val req = HttpRequest.newBuilder()
        .uri(URI("$airtable/${airtableRecord}"))
        .header("Authorization", "Bearer $AIRTABLE_API_KEY")
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
        .build()
    val resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString())
    println(resp)
  }
}

private fun getDocumentUrl(docId: String): String {
  val token = System.getenv("TG_BOT_TOKEN")
  val req = HttpRequest.newBuilder()
      .uri(URI("https://api.telegram.org/bot$token/getFile?file_id=$docId"))
      .GET()
      .build()
  val path = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body()?.let {
    jacksonObjectMapper().readTree(it).let { json ->
      json["result"].get("file_path").asText()
    }
  }
  return "https://api.telegram.org/file/bot$token/$path"
}

private val AIRTABLE_API_KEY = System.getenv("AIRTABLE_API_KEY")
val AIRTABLE_COROUTINE_DISPATCHER = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val AIRTABLE_WITH_SAFES_DEV = "https://api.airtable.com/v0/appx00G1cfwLC98Hs/%D0%94%D0%BE%D1%81%D1%80%D0%BE%D1%87%D0%BA%D0%B0%20%D1%81%20%D0%A1%D0%9F"
private val AIRTABLE_NO_SAFE_DEV = "https://api.airtable.com/v0/appx00G1cfwLC98Hs/%D0%94%D0%BE%D1%81%D1%80%D0%BE%D1%87%D0%BA%D0%B0%20%D0%B1%D0%B5%D0%B7%20%D0%A1%D0%9F"

private val AIRTABLE_WITH_SAFES_PROD = "https://api.airtable.com/v0/app9K6NL1B4lsWXbx/%D0%94%D0%BE%D1%81%D1%80%D0%BE%D1%87%D0%BA%D0%B0%20%20%D1%81%20%D0%A1%D0%9F"
private val AIRTABLE_NO_SAFE_PROD = "https://api.airtable.com/v0/app9K6NL1B4lsWXbx/%D0%94%D0%BE%D1%81%D1%80%D0%BE%D1%87%D0%BA%D0%B0%20%20%D0%B1%D0%B5%D0%B7%20%D0%A1%D0%9F"

private val AIRTABLE_WITH_SAFES = AIRTABLE_WITH_SAFES_PROD
private val AIRTABLE_NO_SAFE = AIRTABLE_NO_SAFE_PROD
