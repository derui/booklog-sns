package models

import java.sql.Date
import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import models._

// Bookテーブルの情報を表すcase class
case class Book(bookId: Long, shelfId: Long, name: String,
                author: Option[String], isbn: Option[String], publishedDate: Option[Date],
                largeImageUrl: Option[String], mediumImageUrl: Option[String],
                smallImageUrl: Option[String],
                created: Timestamp, createdUser: Long,
                updated: Timestamp, updatedUser: Long)

// それぞれの本に対する操作を提供する
object Books extends Table[Book]("book") with Logging {

  type BookWithName = (Book, String, String)

  def bookId = column[Long]("book_id", O.PrimaryKey, O.AutoInc)

  def shelfId = column[Long]("shelf_id")

  def name = column[String]("book_name", O DBType "varchar (200)")

  def author = column[Option[String]]("book_author", O DBType "varchar (200)")

  def isbn = column[Option[String]]("book_isbn", O DBType "varchar (50)")

  def publishedDate = column[Option[Date]]("published_date")

  def largeImageUrl = column[Option[String]]("large_image_url", O DBType "varchar (300)")

  def mediumImageUrl = column[Option[String]]("medium_image_url", O DBType "varchar (300)")

  def smallImageUrl = column[Option[String]]("small_image_url", O DBType "varchar (300)")

  def createdDate = column[Timestamp]("created_date")

  def createdUser = column[Long]("created_user")

  def updatedDate = column[Timestamp]("updated_date")

  def updatedUser = column[Long]("updated_user")

  def * = bookId ~ shelfId ~ name ~ author ~ isbn ~ publishedDate ~ largeImageUrl ~
    mediumImageUrl ~ smallImageUrl ~ createdDate ~ createdUser ~ updatedDate ~ updatedUser <>
    (Book, Book.unapply _)

  def ins = shelfId ~ name ~ author ~ isbn ~ publishedDate ~ largeImageUrl ~
    mediumImageUrl ~ smallImageUrl ~ createdDate ~ createdUser ~ updatedDate ~ updatedUser returning bookId

  /**
   * bookIdに一致するBookを返却する
   * @param bookId 検索対象
   * @param session 暗黙のsession
   * @return 検索したBookとユーザー名
   */
  def selectById(bookId : Long)(implicit ds:Session) : Option[BookWithName] = {
    val query = for {
      b <- Books
      c <- UserInforms
      u <- UserInforms
      if b.bookId === bookId && b.createdUser === c.userId && b.updatedUser === u.userId
    } yield (b, c.userDisplayName, u.userDisplayName)

    log(query)

    query.firstOption
  }

  // 書籍のうち、指定された本棚に入っている本のみを取得する。
  def findAllInShelf(shelfId : Long, start: Option[Int], limits: Option[Int])(implicit session : Session) : List[BookWithName] = {
    val query = for {
      b <- Books.sortBy(_.updatedDate.desc)
      bs <- BookShelves
      u <- UserInforms
      c <- UserInforms
      if b.createdUser === c.userId && b.updatedUser === u.userId && bs.id === b.shelfId &&
      b.shelfId === shelfId
    } yield (b, c.userDisplayName, u.userDisplayName)

    log(query)

    (start, limits) match {
      case (Some(s), None) => query.drop(s).list
      case (None, Some(l)) => query.take(l).list
      case (Some(s), Some(l)) => query.drop(s).take(l).list
      case _ => query.list
    }
  }

  // 指定されたbookIdのBookを削除する
  def delete(id : Long)(implicit session: Session): Int = {
    val q = (for {
       b <- Books
       if b.bookId === id
     } yield (b))
    log(q.deleteStatement)
    q.delete
  }

  def toJson(book: Book): JsValue = {
    implicit val timestampWriter = Writes[Timestamp] {date => Json.toJson("%tF %<tT" format date)}
    implicit val dateWriter = Writes[Date] {date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "book_id").write[Long] and
        (__ \ "shelf_id").write[Long] and
        (__ \ "book_name").write[String] and
        (__ \ "book_author").write[Option[String]] and
        (__ \ "book_isbn").write[Option[String]] and
        (__ \ "published_date").write[Option[Date]] and
        (__ \ "large_image_url").write[Option[String]] and
        (__ \ "medium_image_url").write[Option[String]] and
        (__ \ "small_image_url").write[Option[String]] and
        (__ \ "created_date").write[Timestamp] and
        (__ \ "created_user").write[Long] and
        (__ \ "updated_date").write[Timestamp] and
        (__ \ "updated_user").write[Long])(unlift(Book.unapply))
    Json.toJson(book)
  }
}
