package controllers

import models.Book
import models.BookShelf
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc._
import java.math.BigInteger
import play.api.mvc.Results
import play.api.mvc.SimpleResult
import util._
import models.BookRegister
import models.BookShelf
import models.Book
import models.UserInfo
import models.RentalInfo
import controllers.Connection._

trait Rental extends Controller with JsonResponse with Composable {
  this: Security =>


    // 指定されたレンタル情報を取得する
    def getRentalDetails(id: Long) = Authenticated {
      Action { implicit request =>
        RentalInfo.selectById(BigInteger.valueOf(id)) match {
          case None => BadRequest(Json.obj("error" -> "Not found rental id given"))
          case Some(rental) => OkJsonOneOf(RentalInfo.toJson(rental))
        }
      }
    }

  // レンタル情報の全件を取得する
  def getRentalInfos = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "start" -> optional(number),
          "rows" -> optional(number)))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          val jsoned = RentalInfo.findAll(p._1, p._2).map(RentalInfo.toJson)
          OkJson(jsoned)
        })
    }
  }

  private case class DoRental(user:Long, book:Long)

  // あるユーザーの本ついて、自分がレンタルしたことを示す。
  def doRental = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "rental_user" -> number,
          "rental_book" -> number))

      form.bindFromRequest.fold(
        e => BadRequest(Json.obj("error" -> e.errors.head.message)),
        p => {
          RentalInfo.insert(BigInteger.valueOf(p._1), BigInteger.valueOf(p._2)) match {
            case Left(e) => BadRequest(Json.obj("error" -> e))
            case Right(id) =>
              OkJsonOneOf(Json.obj("rental_id" -> id.longValue))
          }
        }
      )
    }
  }

  // 返却状態に変更する。APIの仕様で、返却状態からレンタル状態に変更することはできない。
  def returnRentalBook(id:Long) = Authenticated {
    Action { implicit request =>
      RentalInfo.update(BigInteger.valueOf(id), false)
      OkJson(List())
    }
  }
}
