package util

import play.api.mvc._
import play.api.libs.json._

object JsonUtil {

  // Json関連の変換処理を提供する
  def listToArray(list:List[JsValue]) : JsArray =
    list.foldLeft(Json.arr())((ary, obj) => ary :+ obj)
}
