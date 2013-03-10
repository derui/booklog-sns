package models

import java.util.Calendar
import java.util.Date
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.math.BigInteger

case class RentalInfo(rentalId: BigInteger,
                      rentalUserId: BigInteger, rentalBookId: BigInteger,
                      rentalNow : Boolean,
                      created: Date, createdUser: String,
                      updated: Date, updatedUser: String)
/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object RentalInfo {

  private val tableName = "RentalInfo"

  val rentalInfo = {
    get[BigInteger]("rental_id") ~
    get[BigInteger]("rental_user_id") ~
    get[BigInteger]("rental_book_id_") ~
    get[Boolean]("rental_now") ~
    get[Date]("created_date") ~
    get[String]("created_user") ~
    get[Date]("updated_date") ~
    get[String]("updated_user") map {
      case id ~ user ~ book ~ flag ~ created ~ cuser ~ updated ~ uuser =>
        RentalInfo(id, user, book, flag, created, cuser, updated, uuser)
    }
  }

  // 新規にレンタル情報を追加する。bookがuserに紐づいていない場合、
  // Leftを返す
  def insert(user: BigInteger, book: BigInteger): Either[String, BigInteger] = {
    val currentDate = Calendar.getInstance().getTime();

    val userInfo = UserInfo.selectById(user)
    val bookInfo = Book.selectById(book)

    (userInfo, bookInfo) match {
      case (None, _) | (_, None) => Left("Given user and book are not relation.")
      case (user, book) =>
        DB.withConnection { implicit connection =>
          SQL("""
              insert into %s (rental_user_id, rental_book_id, rental_now, created_date, created_user,
              updated_date, updated_user)
              values ({user}, {book}, {flag}, {created}, {cuser}, {updated}, {uuser})
              """ format tableName).on("user" -> user, "book" -> book,
                                       "flag" -> "1",
                                       "created" -> currentDate, "updated" -> currentDate,
                                       "cuser" -> "", "uuser" -> "TODO").executeUpdate
          Right(SQL("select last_insert_id() as lastnum from %s" format tableName).apply
          .head[BigInteger]("lastnum"))
        }
    }
  }

  // 指定されたレンタル情報を削除する
  def delete(rental: BigInteger): Int = {
    DB.withConnection { implicit conn =>
      SQL("delete from book_shelf where shelf_id = {id}").on("id" -> shelfId).executeUpdate
    }
  }

  // 全件取得
  def all(start: Option[Int], load: Option[Int]): List[Shelf] = {
    DB.withConnection { implicit conn =>
      (start, load) match {
        case (Some(start), Some(load)) =>
          SQL("select * from book_shelf order by updated_date desc limit {start}, {count}").
        on("start" -> start, "count" -> load).as(shelf *)
        case (None, Some(load)) =>
          SQL("select * from book_shelf order by updated_date limit {count}").on("count" -> load).as(shelf *)
        case _ => SQL("select * from book_shelf").as(shelf *)
      }
    }
  }

  // idが一致する一件だけ取得
  def selectById(id: BigInteger): Option[Shelf] = {
    DB.withConnection { implicit conn =>
      val shelfs = SQL("select * from book_shelf where shelf_id = {id}").on("id" -> id).as(shelf *)
      shelfs match {
        case (s :: _) => Some(s)
        case _ => None
      }
    }
  }

  // 対象をjsonに変換する
  def toJson(target: Shelf): JsValue = {
    implicit val bigIntWriter = Writes[BigInteger] { bi => Json.toJson(bi.longValue()) }
    implicit val dateWriter = Writes[Date] { date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "shelf_id").write[BigInteger] and
      (__ \ "shelf_name").write[String] and
      (__ \ "shelf_description").write[String] and
      (__ \ "created_date").write[Date] and
      (__ \ "created_user").write[String] and
      (__ \ "updated_date").write[Date] and
      (__ \ "updated_user").write[String])(unlift(Shelf.unapply))
    Json.toJson(target)
  }
}
