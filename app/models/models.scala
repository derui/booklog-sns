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
                 updated: Date, updatedUser: String)
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
      get[Date]("updated_date") ~
      get[String]("updated_user") map {
        case id ~ name ~ desc ~ created ~ cuser ~ updated ~ uuser => Shelf(id, name, desc, created, cuser, updated, uuser)
      }
  }

  // 一件追加する。追加後、追加されたshelfのIDを返す
  def insert(name: String, desc: String): BigInteger = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit connection =>
      SQL("""
          insert into book_shelf (shelf_name, shelf_description, created_date, created_user,
          updated_date, updated_user)
          values ({name}, {desc}, {created}, {cuser}, {updated}, {uuser})
          """).on("name" -> name, "desc" -> desc, "created" -> currentDate, "updated" -> currentDate,
        "cuser" -> "TODO", "uuser" -> "TODO").executeUpdate
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
          SQL("select * from book_shelf order by updated_date limit {start}, {count}").
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

case class Book(bookId: BigInteger, shelfId: BigInteger, name: String, author: String,
                isbn: String, created: Date, cuser: String, updated: Date, uuser: String)

// それぞれの本
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
          Book(id, shelf_id, name, author, isbn, created, cuser, updated, uuser)
      }
  }

  // 一件追加
  def insert(book: Book): BigInteger = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit conn =>
      SQL("""
          insert into book (book_name, book_author, book_isbn, created, updated)
          values ({name}, {author}, {isbn}, {created}, {cuser}, {updated}, {uuser})
          """).on("name" -> book.name, "author" -> book.author, "isbn" -> book.isbn,
        "created" -> currentDate, "updated" -> currentDate, "cuser" -> "TODO",
        "uuser" -> "TODO").executeUpdate()
      SQL("select last_insert_id() as lastnum from book").apply.head[BigInteger]("lastnum")
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
  def allInShelf(shelfId: BigInteger, start: Option[Int], load: Option[Int]): List[Book] = {
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
  def selectById(book_id: BigInteger): Option[Book] = {
    DB.withConnection { implicit conn =>
      SQL("select * from book where book_id = {book_id}").on("book_id" -> book_id).as(book *) match {
        case (x :: _) => Some(x)
        case Nil => None
      }
    }
  }

  def toJson(book: Book): JsValue = {
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
      (__ \ "updated_user").write[String])(unlift(Book.unapply))
    Json.toJson(book)
  }
}
