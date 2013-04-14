package controllers

import java.math.BigInteger
import java.net.URI
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Request
import play.api.mvc._
import play.api.mvc.Result
import play.api.mvc.SimpleResult
import controllers.Connection._

// 認証処理を挟みたい場合に、こBのtraitをActionにwithしてやる。
trait Secured extends Security with RequestLogging {
  private var userId = 0L

  override def Authenticated[A](action : Action[A]) : Action[A] = Action(action.parser) { request =>

    // CSRF対策も行う。
    if (checkCRSF(request)) {
      checkAuthorization(request.session) match {
        case Left(e) => e match {
          case ConnectResult.UserNotAuthorizedError(message) => Results.Unauthorized(message)
          case _ => Results.Unauthorized("Given error, but not authrozed error")
        }
        case Right(id) => {
          userId = id
          Logger.info("start : %d : %s with [%s]" format (request.id, request.path, request.queryString.toString))
          val ret = action(request)
          Logger.info("end   : %d : %s with [%s]" format (request.id, request.path, request.queryString.toString))
          ret
        }
      }
    } else {
      Results.BadRequest("Bad Request that maybe given CSRF")
    }
  }

  override def getAuthUserId: Long = userId

  // 単純な恒等関数
  def id[A](x:A) = x

  // CRSF攻撃がおこなわれたかどうかを判定する。行われていた場合は
  // falseが返される。
  private def checkCRSF[A](request : Request[A]) : Boolean = {
    List(
      // Hostと自身のホスト名が同一
      request.headers.get(HeaderNames.HOST).forall(request.host == ),
      // X-Fromヘッダが存在する
      request.headers.get("X-From") match {
        case None => false
        case Some(xFrom) =>
          // オリジンがないか、OriginとX-Fromが同一かどうかをチェックする
          request.headers.get(HeaderNames.ORIGIN) match {
            case None => true
            case Some(header) => {
              val origin = new URI(header)
              val host = new URI(xFrom)
              origin.getScheme == host.getScheme &&
              origin.getHost == host.getHost &&
              origin.getPort == host.getPort
            }
          }
      }
    ).forall(id)
  }
}
