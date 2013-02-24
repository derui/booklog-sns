package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.mvc.Result
import views.html.defaultpages.badRequest
import models.BookShelf

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
      p => {
        println(p)
        insertShelf(p)
      }
    )
    // name match {
    //   case Some((name :: _)) => insertShelf(name)
    //   case Some(_) | None => BadRequest("")
    // }
  }

  def insertShelf(name : String) = {
    BookShelf.insert(name)
    Ok("")
  }

}
