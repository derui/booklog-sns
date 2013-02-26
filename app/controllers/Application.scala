package controllers

import models.Book
import models.BookShelf
import play.api.data.Form
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.tuple
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller

object Application extends Controller {

  def index = Action {
    Ok(views.html.app("title"))
  }

  case class Shelf(name:String)

  def makeShelf = Action { implicit request =>
    val form = Form(
        tuple(
          "shelf_name" -> nonEmptyText,
          "shelf_description" -> nonEmptyText
      )
    )

    form.bindFromRequest.fold(
      e => BadRequest(e.errors.head.message),
      p => {
        BookShelf.insert(p._1, p._2)
        Ok("")
      }
    )
  }

  /**
   * 渡されたJsValueのリストを、返却形式のJsonに変換する
   */
  private def responseToJson(ary:List[JsValue]):JsObject = {
    Json.obj("totalCount" -> ary.length, "result" ->
             ary.foldLeft(Json.arr())((ary, obj) => ary :+ obj)
           )
  }

  // 指定された場合は指定された件数のみ、指定されない場合は全件取得する
  def getAllShelf = Action { implicit request =>
    val form = Form(
      tuple(
        "start" -> optional(number),
        "rows" -> optional(number)
      )
    )

    form.bindFromRequest.fold(
      e => BadRequest(e.errors.head.message),
      p => {
        val jsoned = BookShelf.all(p._1, p._2).map(BookShelf.toJson)
        Ok(responseToJson(jsoned))
      }
    )
  }

  // 1件だけ取得する
  def getShelf(shelf_id:Long) = Action {
    BookShelf.selectById(shelf_id) match {
      case None => Ok(responseToJson(List()))
      case Some(x) => Ok(responseToJson(List(BookShelf.toJson(x))))
    }
  }

  def getBooksInShelf = Action { implicit request =>
    val form = Form(
      tuple(
        "shelf" -> number,
        "start" -> optional(number),
        "rows" -> optional(number)
      )
    )

    form.bindFromRequest.fold(
      e => BadRequest(e.errors.head.message),
      p => {
        val books = Book.allInShelf(p._1, p._2, p._3).map(Book.toJson)
        Ok(responseToJson(books))
      }
    )
  }

  def getShelfDetail(id: Long) = TODO

  def getBookDetial(id: Long) = TODO
}
