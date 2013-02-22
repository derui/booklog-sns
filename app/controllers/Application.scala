package controllers

import play.api._
import play.api.mvc._
import play.api.db.DB
import play.mvc.Result
import views.html.defaultpages.badRequest
import models.BookShelf

object Application extends Controller {

  def index = Action {
    Ok(views.html.app("title"))
  }

  def makeShelf = Action { request =>
    var name = request.queryString.get("name");
    name match {
      case Some((name :: _)) => insertShelf(name)
      case Some(_) | None => BadRequest("")
    }
  }

  def insertShelf(name : String) = {
    BookShelf.insert(name)
    Ok("")
  }

}