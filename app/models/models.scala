package models

import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json

case class Shelf(id:Long, name:String)

object BookShelf {

  val shelf = {
    get[Long]("shelf_id") ~
    get[String]("shelf_name") map {
      case id~name => Shelf(id, name)
    }
  }

  // 一件追加
  def insert(name:String):Any = {
    DB.withConnection { implicit connection =>
      SQL("insert into book_shelf (shelf_name) values ({name})").on("name" -> name).
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
  def select(name:String): (Long, String) = {
    DB.withConnection {implicit conn =>
      SQL("select * from book_shelf where name = {name}").on("name" -> name)().collect {
        case Row(id:Long, name:String) => (id, name)
      }.toList.head
    }
  }
}

case class Book(bookId:Long, shelfId:Long, name:String, author:String, isbn:String)

// それぞれの本
object Book {

  val book = {
    get[Long]("book_id") ~
    get[Long]("shelf_id") ~
    get[String]("book_name") ~
    get[String]("book_author") ~
    get[String]("book_isbn") map {
      case id~shelf_id~name~author~isbn => Book(id, shelf_id, name, author, isbn)
    }
  }

  // 一件追加
  def insert(book:Book) = {
    DB.withConnection {implicit conn =>
      SQL("""
          insert into book (book_name, book_author, book_isbn) values ({name}, {author}, {isbn})
          """).on("name" -> book.name, "author" -> book.author, "isbn" -> book.isbn).
      executeUpdate()
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
