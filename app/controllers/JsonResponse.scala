package controllers

import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import util.Composable
import play.api.mvc._
import play.api.mvc.Results._

// ResponseをJsonで返却する際の振舞いをまとめたtraits
trait JsonResponse {
  /**
   * 渡されたJsValueのリストを、返却形式のJsonに変換する
   */
  def responseToJson(ary: List[JsValue]): JsObject = {
    Json.obj("totalCount" -> ary.length, "result" ->
      ary.foldLeft(Json.arr())((ary, obj) => ary :+ obj))
  }

  def responseToJsonOneOf(value: JsValue) : JsObject =
    responseToJson(List(value))

  def OkJson(ary:List[JsValue]) : PlainResult = Ok(responseToJson(ary))
  def OkJsonOneOf(value:JsValue) : PlainResult = Ok(responseToJsonOneOf(value))
}