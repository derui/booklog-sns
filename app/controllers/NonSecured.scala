package controllers

import java.math.BigInteger
import java.net.URI
import play.api.http.HeaderNames
import play.api.mvc.Request
import play.api.mvc._
import play.api.mvc.Result
import play.api.mvc.SimpleResult
import controllers.Connection._

// テスト用に、認証処理を行わない。
trait NonSecured extends Security {
  override def Authenticated[A](action : Action[A]) : Action[A] = Action(action.parser) (action.apply)
  override def getAuthUserId : BigInteger = BigInteger.valueOf(0L)
}
