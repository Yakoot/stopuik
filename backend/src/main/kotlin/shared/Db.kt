// Copyright (C) 2020 Наблюдатели Петербурга
package org.spbelect.blacklist.shared

import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL.using
import java.sql.ResultSet

val dataSource = HikariDataSource().apply {
  Class.forName("org.postgresql.Driver")
  username = System.getenv("PG_USER") ?: "postgres"
  //username = "hwmxsfem"
  val host = System.getenv("PG_HOST") ?: "localhost"
  //val host = "horton.elephantsql.com"
  val database = System.getenv("PG_DATABASE") ?: username
  //val database = username
  password = System.getenv("PG_PASSWORD") ?: ""
  //  password = "qq6wYuiYNhyEBTup7kVm9i_W_8lrYMPU"
  jdbcUrl = "jdbc:postgresql://$host:5432/$database"
  maximumPoolSize = 3
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

fun <T> db(code: DSLContext.() -> T) =
  using(dataSource, SQLDialect.POSTGRES).use(code)
