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
      "shelf_name" -> nonEmptyText
    )

    form.bindFromRequest.fold(
      e => BadRequest(e.errors.head.message),
      p => { insertShelf(p) }
    )
  }

  def getAllShelf = Action {
    val jsoned = BookShelf.all.map(shelf => Json.obj("id" -> shelf.id, "name" -> shelf.name))
    .foldLeft(Json.arr())((ary, obj) => ary :+ obj)
    Ok(stringifyWithRoot("shelfs", jsoned)
       )
  }

  def insertShelf(name : String) = {
    BookShelf.insert(name)
    Ok("")
  }

  private def stringifyWithRoot(root: String, obj:JsArray) =
    Json.stringify(Json.obj(root -> obj))

  def getBooksInShelf = Action { implicit request =>
    val form = Form(
      "shelf" -> number
    )

    form.bindFromRequest.fold(
      e => BadRequest(e.errors.head.message),
      p => {
        val books = Book.allInShelf(p).map(Book.bookToJson).foldLeft(Json.arr())((ary,obj) => ary :+ obj)
        Ok(stringifyWithRoot("books", books))
      }
    )
  }
}
