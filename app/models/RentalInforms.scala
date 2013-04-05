package models

import java.sql.Date
import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps

case class RentalInform(rentalId: Long, rentalUserId: Long,
  rentalBookId: Long, rentalNow : Boolean,
  created: Timestamp, createdUser: Long,
  updated: Timestamp, updatedUser: Long)

/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object RentalInforms extends Table[RentalInform]("rental_info") with Logging {

  type RentalInfoWithName = (RentalInform, String, String)

  def rentalId = column[Long]("rental_id", O PrimaryKey, O AutoInc)
  def rentalUserId = column[Long]("rental_user_id")
  def rentalBookId = column[Long]("rental_book_id")
  def rentalNow = column[Boolean]("rental_now", O DBType "varchar (1)")
  def createdDate = column[Timestamp]("created_date")
  def createdUser = column[Long]("created_user")
  def updatedDate = column[Timestamp]("updated_date")
  def updatedUser = column[Long]("updated_user")

  def * = rentalId ~ rentalUserId ~ rentalBookId ~ rentalNow ~ createdDate ~ createdUser ~
  updatedDate ~ updatedUser <> (RentalInform, RentalInform.unapply _)

  def ins = rentalUserId ~ rentalBookId ~ rentalNow ~ createdDate ~ createdUser ~
    updatedDate ~ updatedUser returning rentalId

  // rentalIdに一致する一件を取得する
  def selectById(rentalId : Long)(implicit session :Session) : Option[RentalInfoWithName] = {
    val query = for {
      r <- RentalInforms
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId && r.rentalId === rentalId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    query.firstOption
  }

  // bookIdに一致する一件を取得する。
  def selectByBookId(bookId : Long)(implicit session : Session) : Option[RentalInfoWithName] = {
    val query = for {
      r <- RentalInforms
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId && r.rentalBookId === bookId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    query.firstOption
  }

  // 全体から取得する。開始または限度が渡された場合は、それに基づいて取得する。
  def findAll(start: Option[Int], limits:Option[Int])(implicit session : Session) : List[RentalInfoWithName] ={
    val query = for {
      r <- RentalInforms.sortBy(_.updatedDate.desc)
      u <- UserInforms
      c <- UserInforms
      if r.createdUser === c.userId && r.updatedUser === u.userId
    } yield (r, c.userDisplayName, u.userDisplayName)

    log(query)

    (start, limits) match {
      case (Some(s), None) => query.drop(s).list
      case (None, Some(l)) => query.take(l).list
      case (Some(s), Some(l)) => query.drop(s).take(l).list
      case _ => query.list
    }
  }

  // 指定されたレンタル情報を削除する。削除されたレンタル情報は、レンタル履歴に新たに登録される。
  def delete(rentalId: Long)(implicit session :Session) : Int = {
    val query = for {
      r <- RentalInforms
      if r.rentalId === rentalId
    } yield (r)

    log(query)

    val rental = query.first

    log(RentalInformHistories.insertStatement)
    RentalInformHistories.insert(rental)
    query.delete
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
