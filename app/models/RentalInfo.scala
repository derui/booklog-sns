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
                      created: Date, createdUser: String, createdUserName : String,
                      updated: Date, updatedUser: String, updatedUserName : String)
/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object RentalInfo {

  private val tableName = "RentalInfo"

  val rentalInfo = {
    get[BigInteger]("rental_id") ~
    get[BigInteger]("rental_user_id") ~
    get[BigInteger]("rental_book_id") ~
    get[String]("rental_now") ~
    get[Date]("created_date") ~
    get[String]("created_user") ~
    get[String]("user_display_name") ~
    get[Date]("updated_date") ~
    get[String]("updated_user") ~
    get[String]("user_display_name") map {
      case id ~ user ~ book ~ "1" ~ created ~ cuser ~ cusername~updated ~ uuser~uusername =>
        RentalInfo(id, user, book, true, created, cuser, cusername, updated, uuser, uusername)
        case id ~ user ~ book ~ "0" ~ created ~ cuser~cusername~updated~uuser~uusername =>
        RentalInfo(id, user, book, false, created, cuser, cusername, updated, uuser, uusername)
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
      case (Some(_), Some(_)) =>
        DB.withConnection { implicit connection =>
         SQL("""
              insert into %s (rental_user_id, rental_book_id, rental_now, created_date, created_user,
              updated_date, updated_user)
              values ({user}, {book}, {flag}, {created}, {cuser}, {updated}, {uuser})
              """ format tableName).on("user" -> user, "book" -> book,
                                       "flag" -> "1",
                                       "created" -> currentDate, "updated" -> currentDate,
                                       "cuser" -> user.toString, "uuser" -> user.toString).executeUpdate
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

  private val commonSelect = """select
    r.rental_id,
    r.rental_user_id,
    r.rental_book_id,
    r.rental_now,
    r.created_date,
    r.created_user,
    c.user_display_name,
    r.updated_date,
    r.updated_user,
    u.user_display_name
  from RentalInfo r join UserInfo c on
    c.user_id = r.created_user
  join UserInfo u on
    u.user_id = r.updated_user
"""

  // 全件取得
  def findAll(start: Option[Int], load: Option[Int]): List[RentalInfo] = {
    DB.withConnection { implicit conn =>
      (start, load) match {
        case (Some(start), Some(load)) =>
          SQL(commonSelect + "order by updated_date desc limit {start}, {count}"
            ).on("start" -> start, "count" -> load).as(rentalInfo *)
        case (None, Some(load)) =>
          SQL(commonSelect + "order by updated_date limit {count}"
            ).on("count" -> load).as(rentalInfo *)
        case _ => SQL(commonSelect).as(rentalInfo *)
      }
    }
  }

  // idが一致する一件だけ取得
  def selectById(id: BigInteger): Option[RentalInfo] = {
    DB.withConnection { implicit conn =>
      val shelfs = SQL(commonSelect + "where rental_id = {id}")
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
        (__ \ "created_user_name").write[String] and
        (__ \ "updated_date").write[Date] and
        (__ \ "updated_user").write[String] and
        (__ \ "updated_user_name").write[String])(unlift(RentalInfo.unapply))
    Json.toJson(target)
  }
}
