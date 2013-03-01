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

// Bookテーブルの情報を表すcase class
case class BookDetail(bookId: BigInteger, shelfId: BigInteger, name: String, author: String,
                      isbn: String, created: Date, cuser: String, updated: Date, uuser: String)

// Bookを作成する際に必要な情報を表すcase class
case class BookRegister(shelfId: BigInteger, name: String, author: String, isbn: String)

// それぞれの本に対する操作を提供する
object Book {

  val book = {
    get[BigInteger]("book_id") ~
      get[BigInteger]("shelf_id") ~
      get[String]("book_name") ~
      get[String]("book_author") ~
      get[String]("book_isbn") ~
      get[Date]("created_date") ~
      get[String]("created_user") ~
      get[Date]("updated_date") ~
      get[String]("updated_user") map {
        case id ~ shelf_id ~ name ~ author ~ isbn ~ created ~ cuser ~ updated ~ uuser =>
          BookDetail(id, shelf_id, name, author, isbn, created, cuser, updated, uuser)
      }
  }

  // 一件追加
  def insert(book: BookRegister): Either[String, BigInteger] = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit conn =>
      BookShelf.selectById(book.shelfId) match {
        case None => Left("Shelf not found")
        case Some(_) =>
          SQL("""
          insert into book (shelf_id, book_name, book_author, book_isbn, created_date, created_user, updated_date, updated_user)
          values ({id}, {name}, {author}, {isbn}, {created}, {cuser}, {updated}, {uuser})
          """).on("id" -> book.shelfId, "name" -> book.name, "author" -> book.author, "isbn" -> book.isbn,
            "created" -> currentDate, "updated" -> currentDate, "cuser" -> "TODO",
            "uuser" -> "TODO").executeUpdate()
          Right(SQL("select last_insert_id() as lastnum from book").apply.head[BigInteger]("lastnum"))
      }
    }
  }

  // IDに一致するbookを一件削除する
  def delete(bookId: BigInteger): Int = {
    DB.withConnection { implicit conn =>
      SQL("delete from book where book_id = {id}").on("id" -> bookId).executeUpdate
    }
  }

  /**
   * 指定した本棚に関連付けられた本を取得する。
   *
   * @param BigInteger shelfId 取得対象の本棚ID
   * @param Option[Int] 取得を開始する位置
   * @param Option[Int] 取得件数
   * @return List[Book] 取得した本
   */
  def allInShelf(shelfId: BigInteger, start: Option[Int], load: Option[Int]): List[BookDetail] = {
    val commonSql = "select * from book where shelf_id = {shelf_id} order by updated_date"
    DB.withConnection { implicit conn =>
      (start, load) match {
        case (Some(start), Some(load)) =>
          SQL(commonSql + " limit {offset}, {count}").on(
            "shelf_id" -> shelfId, "offset" -> start, "count" -> load).as(book *)
        case (None, Some(load)) =>
          SQL(commonSql + " limit {count}").on("shelf_id" -> shelfId, "count" -> load).as(book *)
        case _ =>
          SQL("select * from book where shelf_id = {shelf_id}").on(
            "shelf_id" -> shelfId).as(book *)
      }
    }
  }

  // book_idが一致する一件だけ取得する
  def selectById(book_id: BigInteger): Option[BookDetail] = {
    DB.withConnection { implicit conn =>
      SQL("select * from book where book_id = {book_id}").on("book_id" -> book_id).as(book *) match {
        case (x :: _) => Some(x)
        case Nil => None
      }
    }
  }

  def toJson(book: BookDetail): JsValue = {
    implicit val bigIntWriter = Writes[BigInteger] { bi => Json.toJson(bi.longValue()) }
    implicit val writer = (
      (__ \ "book_id").write[BigInteger] and
      (__ \ "shelf_id").write[BigInteger] and
      (__ \ "book_name").write[String] and
      (__ \ "book_author").write[String] and
      (__ \ "book_isbn").write[String] and
      (__ \ "created_date").write[Date] and
      (__ \ "created_user").write[String] and
      (__ \ "updated_date").write[Date] and
      (__ \ "updated_user").write[String])(unlift(BookDetail.unapply))
    Json.toJson(book)
  }
}
