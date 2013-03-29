package util

import play.api.mvc._
import play.api.libs.json._

object JsonUtil {

  // Json関連の変換処理を提供する
  def listToArray(list: List[JsValue]): JsArray =
    list.foldLeft(Json.arr())((ary, obj) => ary :+ obj)

  // 新規作成ユーザー名と更新ユーザー名を、baseの変換後のjsonに付加した結果を返す。
  def jsonWithUserName[A, B <: JsValue](base: A, createdUserName: String, updatedUserName: String, f:(A) => B): JsValue = {

    val transformer = (__).json.update(
      __.read[JsObject].map {
        o => o ++ Json.obj("created_user_name" -> createdUserName) ++ Json.obj("updated_user_name" -> updatedUserName)
      })
    f(base).transform(transformer).get
  }
}
