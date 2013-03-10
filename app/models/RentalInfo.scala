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
  // Leftを返す。
  // ここでのuserは、レンタルする側のuserで、bookは別のユーザーが保持するbookとなる。
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
  def delete(rentalId: BigInteger): Int = {
    DB.withConnection { implicit conn =>
      SQL("delete from %s where rental_id = {id}" format tableName).on("id" -> rentalId).executeUpdate
    }
  }

  // 全件取得
  def findAll(start: Option[Int], load: Option[Int]): List[RentalInfo] = {
    DB.withConnection { implicit conn =>
      (start, load) match {
        case (Some(start), Some(load)) =>
          SQL("select * from %s order by updated_date desc limit {start}, {count}"
              format tableName
            ).on("start" -> start, "count" -> load).as(rentalInfo *)
        case (None, Some(load)) =>
          SQL("select * from %s order by updated_date limit {count}" format tableName
            ).on("count" -> load).as(rentalInfo *)
        case _ => SQL("select * from %s" format tableName).as(rentalInfo *)
      }
    }
  }

  // idが一致する一件だけ取得
  def selectById(id: BigInteger): Option[RentalInfo] = {
    DB.withConnection { implicit conn =>
      val shelfs = SQL("select * from %s where rental_id = {id}" format tableName)
      .on("id" -> id).as(rentalInfo *)
      shelfs match {
        case (s :: _) => Some(s)
        case _ => None
      }
    }
  }

  // 対象をjsonに変換する
  def toJson(target: RentalInfo): JsValue = {
    implicit val bigIntWriter = Writes[BigInteger] { bi => Json.toJson(bi.longValue()) }
    implicit val dateWriter = Writes[Date] { date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "rental_id").write[BigInteger] and
      (__ \ "rental_user_id").write[BigInteger] and
      (__ \ "rental_book_id").write[BigInteger] and
      (__ \ "rental_now").write[Boolean] and
      (__ \ "created_date").write[Date] and
      (__ \ "created_user").write[String] and
      (__ \ "updated_date").write[Date] and
      (__ \ "updated_user").write[String])(unlift(RentalInfo.unapply))
    Json.toJson(target)
  }
}
