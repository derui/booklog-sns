package models

import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import models.DBWrap._

case class BookShelf(id: Long, name: String, description: String, created: Timestamp,
                 createdUser: Long, updated: Timestamp, updatedUser: Long)

/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object BookShelves extends Table[BookShelf]("book_shelf") with Logging {

  type BookShelfWithName = (BookShelf, String, String)

  def id = column[Long]("shelf_id", O PrimaryKey, O.AutoInc)

  def name = column[String]("shelf_name", O DBType "varchar (50)")

  def description = column[String]("shelf_description", O DBType "varchar (100)")

  def createdDate = column[Timestamp]("created_date")

  def createdUser = column[Long]("created_user")

  def updatedDate = column[Timestamp]("updated_date")

  def updatedUser = column[Long]("updated_user")

  def * = id ~ name ~ description ~ createdDate ~ createdUser ~ updatedDate ~ updatedUser <>
    (BookShelf, BookShelf.unapply _)

  def ins = name ~ description ~ createdDate ~ createdUser ~ updatedDate ~ updatedUser returning id

  def selectById(shelfId : Long)(implicit session : Session) : Option[BookShelfWithName] = {
    val query = for {
      b <- BookShelves
      u <- UserInforms
      c <- UserInforms
      if b.createdUser === c.userId && b.updatedUser === u.userId && b.id === shelfId
    } yield (b, c.userDisplayName, u.userDisplayName)

    log(query)

    query.firstOption
  }

  // 全体から取得する。開始または限度が渡された場合は、それに基づいて取得する。
  def findAll(start: Option[Int], limits:Option[Int])(implicit session : Session) : List[BookShelfWithName] ={
    val query = for {
      bs <- BookShelves.sortBy(_.updatedDate.desc)
      u <- UserInforms
      c <- UserInforms
      if bs.createdUser === c.userId && bs.updatedUser === u.userId
    } yield (bs, c.userDisplayName, u.userDisplayName)

    log(query)

    (start, limits) match {
      case (Some(s), None) => query.drop(s).list
      case (None, Some(l)) => query.take(l).list
      case (Some(s), Some(l)) => query.drop(s).take(l).list
      case _ => query.list
    }
  }

  // 対象の本棚を削除する。
  def delete(shelfId :Long)(implicit session:Session): Int = {
    val q = (for {b <- BookShelves if b.id === shelfId} yield b)
    log(q)
    q.delete
  }

  
  // 対象をjsonに変換する
  def toJson(target: BookShelf): JsValue = {
    implicit val dateWriter = Writes[Timestamp] {date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "shelf_id").write[Long] and
        (__ \ "shelf_name").write[String] and
        (__ \ "shelf_description").write[String] and
        (__ \ "created_date").write[Timestamp] and
        (__ \ "created_user").write[Long] and
        (__ \ "updated_date").write[Timestamp] and
        (__ \ "updated_user").write[Long])(unlift(BookShelf.unapply))
    Json.toJson(target)
  }
}
