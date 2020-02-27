// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist

import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

val dataSource = HikariDataSource().apply {
  Class.forName("org.postgresql.Driver")
  username = System.getenv("PG_USER") ?: "postgres"
  val host = System.getenv("PG_HOST") ?: "localhost"
  val database = System.getenv("PG_DATABASE") ?: username
  password = System.getenv("PG_PASSWORD") ?: ""
  jdbcUrl = "jdbc:postgresql://$host:5432/$database"
  maximumPoolSize = 5
}

fun <T> executeQuery(query: String, code: (ResultSet) -> T): T {
  return dataSource.connection.use { conn ->
    conn.createStatement().use { stmt ->
      stmt.executeQuery(query).use { rs ->
        code(rs)
      }
    }
  }
}
