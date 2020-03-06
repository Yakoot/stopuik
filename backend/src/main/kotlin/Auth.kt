package org.spbelect.blacklist

import com.google.api.client.http.HttpStatusCodes
import com.google.api.server.spi.ServiceException
import com.google.api.server.spi.auth.common.User
import com.google.api.server.spi.config.Authenticator
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

class DoSignIn : HttpServlet() {
  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    println("password="+req.getParameter("password") + " user="+req.getParameter("username"))
    req.getSession(true).setAttribute("userId", req.getParameter("username"))
    resp.status = HttpStatusCodes.STATUS_CODE_OK
  }
}
