package org.spbelect.blacklist

import com.google.api.client.http.HttpStatusCodes
import com.google.api.server.spi.ServiceException
import com.google.api.server.spi.auth.common.User
import com.google.api.server.spi.config.Authenticator
import com.google.common.io.BaseEncoding
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*
import org.spbelect.blacklist.shared.dataSource
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author dbarashev@bardsoftware.com
 */
class AccessTokenAuthenticator : Authenticator {
  override fun authenticate(request: HttpServletRequest): User? {
    return request.getSession(true).getAttribute("userId")?.let {
      User(it.toString(), "narushitel.net@gmail.com")
    } ?: throw ServiceException(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, "Please sign in")
  }
}

class CheckSignIn : HttpServlet() {
  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    val session = req.getSession(false)
    resp.status = session?.getAttribute("userId")?.let {
      HttpStatusCodes.STATUS_CODE_OK
    } ?: HttpStatusCodes.STATUS_CODE_UNAUTHORIZED
  }
}

enum class Permission {
  READER, WRITER
}

class DoSignIn : HttpServlet() {
  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val email = req.getParameter("email")
    if (email == null) {
      resp.sendError(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, "Не указан email")
      return
    }

    using(dataSource, SQLDialect.POSTGRES).use { ctx ->
      ctx.select(
          field("uid"),
          field("name"),
          field("permission"))
          .from(table("RegistryUser"))
          .where(field("email").equalIgnoreCase(email))
          .fetchOne()?.let {row ->
            req.getSession(true).let {session ->
              session.setAttribute("userId", row["uid"].toString())
              session.setAttribute("created", Date().time)
            }
            resp.addCookie(Cookie("userName", BaseEncoding.base64().encode(row["name"].toString().toByteArray())))
            resp.status = HttpStatusCodes.STATUS_CODE_OK
          } ?: {
            resp.sendError(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "Пользователь $email не найден")
          }()
    }
  }
}
