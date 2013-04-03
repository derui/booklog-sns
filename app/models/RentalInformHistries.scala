package models

import java.sql.Date
import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps

/**
 * レンタル情報についての履歴を管理するテーブル。レンタル情報と制約以外は同一の
 * インターフェースを保持する。
 */
object RentalInformHistories extends Table[RentalInform]("rental_info_history") with Logging {

  type RentalInfoWithName = (RentalInform, String, String)
  
  def rentalId = column[Long]("rental_id")
  def rentalUserId = column[Long]("rental_user_id")
  def rentalBookId = column[Long]("rental_book_id")
  def rentalNow = column[Boolean]("rental_now", O DBType "varchar (1)")
  def createdDate = column[Timestamp]("created_date")
  def createdUser = column[Long]("created_user")
  def updatedDate = column[Timestamp]("updated_date")
  def updatedUser = column[Long]("updated_user")

  def * = rentalId ~ rentalUserId ~ rentalBookId ~ rentalNow ~ createdDate ~ createdUser ~
  updatedDate ~ updatedUser <> (RentalInform, RentalInform.unapply _)

  // rentalIdの履歴を取得する。
  def selectById(rentalId : Long)(implicit session :Session) : List[RentalInfoWithName] = {
    val query = for {
      r <- RentalInformHistories
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId && r.rentalId === rentalId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    query.list
  }

  // bookIdに一致する履歴を取得する
  def selectByBookId(bookId : Long)(implicit session : Session) : List[RentalInfoWithName] = {
    val query = for {
      r <- RentalInformHistories
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId && r.rentalBookId === bookId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    query.list
  }

  // userIdに一致する履歴を取得する
  def selectByUserId(userId : Long)(implicit session : Session) : List[RentalInfoWithName] = {
    val query = for {
      r <- RentalInformHistories
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId && r.rentalUserId === userId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    query.list
  }

  // 対象をjsonに変換する
  def toJson(target: RentalInform): JsValue = {
    implicit val dateWriter = Writes[Timestamp] { date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "rental_id").write[Long] and
        (__ \ "rental_user_id").write[Long] and
        (__ \ "rental_book_id").write[Long] and
        (__ \ "rental_now").write[Boolean] and
        (__ \ "created_date").write[Timestamp] and
        (__ \ "created_user").write[Long] and
        (__ \ "updated_date").write[Timestamp] and
        (__ \ "updated_user").write[Long])(unlift(RentalInform.unapply))
    Json.toJson(target)
  }
}
