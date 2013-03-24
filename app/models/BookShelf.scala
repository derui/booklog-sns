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

case class Shelf(id: BigInteger, name: String, description: String, created: Date, createdUser: String,
  createdUserName: String,
  updated: Date, updatedUser: String, updatedUserName: String)
/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object BookShelf {

  val shelf = {
    get[BigInteger]("shelf_id") ~
    get[String]("shelf_name") ~
    get[String]("shelf_description") ~
    get[Date]("created_date") ~
    get[String]("created_user") ~
    get[String]("user_display_name") ~
    get[Date]("updated_date") ~
    get[String]("updated_user") ~
    get[String]("user_display_name")map {
        case id ~ name ~ desc ~ created ~ cuser ~ cusername~updated~uuser~uusername =>
        Shelf(id, name, desc, created, cuser, cusername, updated, uuser, uusername)
      }
  }

  // 一件追加する。追加後、追加されたshelfのIDを返す
  def insert(name: String, desc: String, userId : BigInteger): BigInteger = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit connection =>
      SQL("""
          insert into book_shelf (shelf_name, shelf_description, created_date, created_user,
          updated_date, updated_user)
          values ({name}, {desc}, {created}, {cuser}, {updated}, {uuser})
          """).on('name -> name, 'desc -> desc, 'created -> currentDate, 'updated -> currentDate,
        'cuser -> userId.toString, 'uuser -> userId.toString).executeUpdate
      SQL("select last_insert_id() as lastnum from book_shelf").apply.head[BigInteger]("lastnum")
    }
  }

  // 指定されたshelfを削除する
  def delete(shelfId: BigInteger): Int = {
    DB.withConnection { implicit conn =>
      SQL("delete from book_shelf where shelf_id = {id}").on("id" -> shelfId).executeUpdate
    }
  }

  // 全件取得
  def all(start: Option[Int], load: Option[Int]): List[Shelf] = {
    DB.withConnection { implicit conn =>
      (start, load) match {
        case (Some(start), Some(load)) =>
          SQL(commonSelect + "order by b.updated_date desc limit {start}, {count}").
            on("start" -> start, "count" -> load).as(shelf *)
        case (None, Some(load)) =>
          SQL(commonSelect + " order by b.updated_date desc limit {count}").on("count" -> load).as(shelf *)
        case _ => SQL(commonSelect).as(shelf *)
      }
    }
  }

  private val commonSelect: String = """select
    b.shelf_id,
    b.shelf_name,
    b.shelf_description,
    b.created_date,
    b.created_user,
    c.user_display_name,
    b.updated_date,
    b.updated_user,
    u.user_display_name
from book_shelf b join UserInfo c on
  c.user_id = b.created_user
join UserInfo u on
  u.user_id = b.updated_user
 """

  // idが一致する一件だけ取得
  def selectById(id: BigInteger): Option[Shelf] = {
    DB.withConnection { implicit conn =>
      val shelfs = SQL(commonSelect + "where shelf_id = {id}").on("id" -> id).as(shelf *)
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
        (__ \ "created_user_name").write[String] and
        (__ \ "updated_date").write[Date] and
        (__ \ "updated_user").write[String] and
        (__ \ "updated_user_name").write[String])(unlift(Shelf.unapply))
    Json.toJson(target)
  }
}
