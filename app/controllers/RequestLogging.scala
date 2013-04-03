package controllers

import play.api.Logger
import play.api.mvc.Action

// Actionの実行時のログを追加するtrait
trait RequestLogging {

  def requestWrap[A](action:Action[A]) : Action[A] = Action(action.parser) {request =>
    Logger.info("start : %d : %s with [%s]" format (request.id, request.path, request.rawQueryString))
    val ret = action(request);
    Logger.info("end   : %d : %s with [%s]" format (request.id, request.path, request.rawQueryString))
    ret
  }
}
