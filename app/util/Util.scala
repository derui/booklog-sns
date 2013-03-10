package util

import play.api.mvc._
import java.math.BigInteger

trait ComposableFunction1[-T1, +R] {
  val f: T1 => R
  def >>[A](g: R => A): T1 => A = f andThen g
  def <<[A](g: A => T1): A => R = f compose g

}

trait Composable {
  // composeの組み合わせを、<</>>で制御できるようにする
  implicit def toComposableFunction1[T1, R](func: T1 => R) = new ComposableFunction1[T1, R] { val f = func }
}

object Util {
  // セッション内に存在するユーザーIDを取得する。これを呼び出せるタイミングは、
  // applyの内部しか無い。
  def userIdInSession[A](request:Request[A]) : BigInteger =
    controllers.Connection.checkAuthorization(request.session).right.get
}
