package controllers

import java.sql.Timestamp
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc._
import util._
import play.api.libs.json._
import scala.Some
import models.DBWrap.UsePerDB
import java.util.Calendar
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}

/**
 * レンタル状況の登録、変更、取得APIへのアクセスを受ける。
 * ここで受けたものは、基本的にModelかLogicに丸投げする。
 */
trait Rental extends Controller with JsonResponse with Composeable with UsePerDB {
  this: Security =>


  // 指定されたレンタル情報を取得する
  def getRentalDetails(id: Long) = Authenticated {
    Action {
      implicit request =>
        db.withSession {
          implicit ds =>
            RentalInforms.selectById(id).flatMap(jsonize _) match {
              case None => error("指定されたレンタル情報は存在しません")
              case Some(rental) => okJsonOneOf(rental)
            }
        }
    }
  }

  // 渡されたレンタル情報から、書籍情報も取得して返却する。
  private def jsonize(rentalWithName: RentalInforms.RentalInfoWithName)(implicit ss: DBSession): Option[JsValue] = {
    val (rental, createdUserName, updatedUserName) = rentalWithName
    Books.selectById(rental.rentalBookId).map {
      case (book, _, _) =>
        val rentalJson =
          rentalWithName match {
            case (rental, cname, uname) => JsonUtil.jsonWithUserName(rental, cname, uname, RentalInforms.toJson _)
          }

        val transformer = (__).json.update(
          __.read[JsObject].map {
            o => o ++ Json.obj("large_image_url" -> book.largeImageUrl,
              "medium_image_url" -> book.mediumImageUrl,
              "small_image_url" -> book.smallImageUrl)
          })
        rentalJson.transform(transformer).get
    }
  }

  // レンタル情報の全件を取得する
  def getRentalInformAll = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          tuple(
            "start" -> optional(number),
            "rows" -> optional(number)))

        form.bindFromRequest.fold(
          e => error(e.errors.head.message),
          p => {
            db.withSession {
              implicit ds =>
                okJson(RentalInforms.findAll(p._1, p._2).map(jsonize(_))
                  .filterNot(x => x.isEmpty).map(_.get))
            }
          }
        )
    }
  }

  // あるユーザーの本について、自分がレンタルしたことを示す。
  def doRental() = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          "rental_book" -> number)

        form.bindFromRequest.fold(
          e => error(e.errors.head.message),
          p => {
            db withTransaction {
              implicit ds =>
                val now = new Timestamp(Calendar.getInstance().getTimeInMillis)
                val id = RentalInforms.ins.insert(getAuthUserId,
                  p, true, now, getAuthUserId, now, getAuthUserId)
                okJsonOneOf(Json.obj("rental_id" -> id))
            }
          })
    }
  }

  // 返却状態に変更する。返却の際には、レンタル情報は削除される。
  def returnRentalBook(id: Long) = Authenticated {
    Action {
      implicit request =>
        db withTransaction {
          implicit ds: DBSession =>
            val q = for {
              r <- RentalInforms
              if r.rentalId === id} yield r
            q.delete
        }
        okJson(List())
    }
  }

  def getRentalInfoByBookId = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          "book_id" -> number)

        form.bindFromRequest.fold(
          e => error(e.errors.head.message),
          p => {
            db withSession {
              implicit ds =>
                RentalInforms.selectByBookId(p) match {
                  case None => okEmpty()
                  case Some(rental) => okJsonOneOf(jsonize(rental).get)
                }
            }
          })
    }
  }
}

