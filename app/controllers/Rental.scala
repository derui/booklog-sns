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

/**
 * レンタル状況の登録、変更、取得APIへのアクセスを受ける。
 * ここで受けたものは、基本的にModelかLogicに丸投げする。
 */
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
          val jsoned = RentalInfo.findAll(p._1, p._2).map { rental =>
            Book.selectById(rental.rentalBookId) match {
              case None => Json.obj()
              case Some(book) =>
                Json.obj("rental_id" -> rental.rentalId.longValue,
                         "rental_user_id" -> rental.rentalUserId.longValue,
                         "rental_book_id" -> rental.rentalBookId.longValue,
                         "rental_now" -> rental.rentalNow,
                         "created_date" -> rental.created,
                         "created_user" -> rental.createdUser,
                         "updated_date" -> rental.updated,
                         "updated_user" -> rental.updatedUser,
                         "large_image_url" -> book.largeImageUrl,
                         "medium_image_url" -> book.mediumImageUrl,
                         "small_image_url" -> book.smallImageUrl
                       )
            }
          }
          OkJson(jsoned)
        })
    }
  }

  private case class DoRental(user:Long, book:Long)

  // あるユーザーの本ついて、自分がレンタルしたことを示す。
  def doRental = Authenticated {
    Action { implicit request =>
      val form = Form(
          "rental_book" -> number)

      form.bindFromRequest.fold(
        e => BadRequest(Json.obj("error" -> e.errors.head.message)),
        p => {
          RentalInfo.insert(getAuthUserId,
                            BigInteger.valueOf(p)) match {
            case Left(e) => BadRequest(Json.obj("error" -> e))
            case Right(id) =>
              OkJsonOneOf(Json.obj("rental_id" -> id.longValue))
          }
        }
      )
    }
  }

  // 返却状態に変更する。返却の際には、レンタル情報は削除される。
  def returnRentalBook(id:Long) = Authenticated {
    Action { implicit request =>
      RentalInfo.delete(BigInteger.valueOf(id))
      OkJson(List())
    }
  }
}
