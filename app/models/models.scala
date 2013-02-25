package models

import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json
import java.sql.Timestamp
import play.libs.Time
import java.util.Date
import java.util.Calendar

case class Shelf(id:Long, name:String, description:String, created:Date, updated:Date)

/**
 * 本棚データベースに対する操作をまとめたオブジェクト
 */
object BookShelf {

  val shelf = {
    get[Long]("shelf_id") ~
    get[String]("shelf_name") ~
    get[String]("shelf_description") ~
    get[Date]("created_date") ~
    get[Date]("updated_date") map {
      case id~name~desc~created~updated => Shelf(id, name, desc, created, updated)
    }
  }

  // 一件追加
  def insert(name:String, desc:String):Any = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit connection =>
      SQL("""
          insert into book_shelf (shelf_name, shelf_description, created_date, updated_date)
          values ({name}, {desc}, {created}, {updated})
          """).on("name" -> name, "desc" -> desc, "created" -> currentDate, "updated" -> currentDate).
      executeUpdate()
    }
  }

  // 全件取得
  def all: List[Shelf] = {
    DB.withConnection {implicit conn =>
      SQL("select * from book_shelf").as(shelf *)
    }
  }

  // 一件だけ取得
  def select(name:String): Shelf = {
    DB.withConnection {implicit conn =>
      val shelfs = SQL("select * from book_shelf where shelf_name = {name}").on("name" -> name).as(shelf *)
      shelfs match {
        case (s :: _) => s
        case _ => throw new Exception("not found")
      }
    }
  }
}

case class Book(bookId:Long, shelfId:Long, name:String, author:String, isbn:String,  created:Date, updated:Date)

// それぞれの本
object Book {

  val book = {
    get[Long]("book_id") ~
    get[Long]("shelf_id") ~
    get[String]("book_name") ~
    get[String]("book_author") ~
    get[String]("book_isbn") ~
    get[Date]("created_date") ~
    get[Date]("updated_date") map {
      case id~shelf_id~name~author~isbn~created~updated => Book(id, shelf_id, name, author, isbn, created, updated)
    }
  }

  // 一件追加
  def insert(book:Book) = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection {implicit conn =>
      SQL("""
          insert into book (book_name, book_author, book_isbn, created, updated)
          values ({name}, {author}, {isbn}, {created}, {updated})
          """).on("name" -> book.name, "author" -> book.author, "isbn" -> book.isbn,
              "created" -> currentDate, "updated" -> currentDate).executeUpdate()
    }
  }

  def allInShelf(shelfId:Long) : List[Book] = {
    DB.withConnection { implicit conn =>
      SQL("select * from book where shelf_id = {shelf_id}").on(
        "shelf_id" -> shelfId
      ).as(book *)
    }
  }

  def bookToJson(book:Book) = {
    Json.obj("id" -> book.bookId, "shelf_id" -> book.shelfId,
             "name" -> book.name, "author" -> book.author, "isbn" -> book.isbn
           )
  }
}
