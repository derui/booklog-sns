package controllers

import java.math.BigInteger
import play.api.mvc._
import play.api.mvc.Result
import play.api.mvc.SimpleResult
import controllers.Connection._

// 認証処理を挟みたい場合に、こBのtraitをActionにwithしてやる。
trait Secured extends Security {
  override def Authenticated[A](action : Action[A]) : Action[A] = Action(action.parser) { request =>
    checkAuthorization(request.session) match {
      case Left(e) => e match {
        case ConnectResult.UserNotAuthorizedError(message) => Results.Unauthorized(message)
        case _ => Results.Unauthorized("Given error, but not authrozed error")
      }
      case Right(_) => action(request)
    }
  }
}
