package logics

import java.sql.Date
import java.sql.Timestamp
import java.util.Calendar
import models.Book
import models.BookShelf
import models.BookShelves
import models.Books
import models.Logging
import play.api.Logger
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import models.DBWrap

object Register {
  case class Book(name :String, author:Option[String], isbn:Option[String],
    published : Option[Date], largeUrl : Option[String],
    mediumUrl : Option[String], smallUrl : Option[String], user : Long)
}

class BookShelfLogic extends DBWrap with Logging {
  type ShelfId = Long
  type BookId = Long

  def addBook(shelfId :ShelfId, book:Register.Book) : Either[String, BookId] = {

    // 指定されたShelfが存在しない場合にはそもそも登録を行わせない
    getShelf(shelfId) match {
      case None => Left("指定された本棚が存在しません")
      case Some(_) =>
        db.withTransaction {
          implicit ds =>
          val now = new Timestamp(Calendar.getInstance().getTimeInMillis)
          val result = Books.ins.insert(shelfId, book.name, book.author, book.isbn,
            book.published, book.largeUrl, book.mediumUrl, book.smallUrl, now,
            book.user, now, book.user)

          log(Books.ins.insertStatementFor(shelfId, book.name, book.author, book.isbn,
            book.published, book.largeUrl, book.mediumUrl, book.smallUrl, now,
            book.user, now, book.user).toString)

          // 追加した本棚についても、更新したとみなす。
          val q = for {bs <- BookShelves
            if bs.id === shelfId
          } yield bs
          q.map(_.updatedDate).update(now)
          log(q.updateStatement)
          q.map(_.updatedUser).update(book.user)
          log(q.updateStatement)

          Right(result)
        }
    }
  }

  def getShelf(shelfId : ShelfId) : Option[BookShelves.BookShelfWithName] =
    db.withSession {implicit session =>
      BookShelves.selectById(shelfId)
    }

  def getBook(bookId : BookId) : Option[Books.BookWithName] =
    db.withSession {implicit session =>
      Books.selectById(bookId)
    }
}
