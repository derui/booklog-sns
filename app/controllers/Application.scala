package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.mvc.Result
import models.BookShelf
import models.Book
import play.api.libs.json._

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
      p => { insertShelf(p._1, p._2) }
    )
  }

  private def responseJson(ary:List[JsValue]):JsObject = {
    Json.obj("totalCount" -> ary.length, "result" ->
             ary.foldLeft(Json.arr())((ary, obj) => ary :+ obj)
           )
  }

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
        val jsoned = BookShelf.all(p._1, p._2).map(shelf => Json.obj("id" -> shelf.id, "name" -> shelf.name))
        Ok(responseJson(jsoned))
      }
    )
  }

  def insertShelf(name : String, desc:String) = {
    BookShelf.insert(name, desc)
    Ok("")
  }

  private def stringifyWithRoot(root: String, obj:JsArray) =
    Json.stringify(Json.obj(root -> obj))

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
        val books = Book.allInShelf(p._1, p._2, p._3).map(Book.bookToJson)
        Ok(responseJson(books))
      }
    )
  }
}
