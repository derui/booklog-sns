package controllers

import play.api.mvc.Action

// 認証処理を加えたActionを実行するためのtrait
trait Security {
  def Authenticated[A](action:Action[A]) : Action[A]
}
