package test

import db.wrapper.specs2.AutoRollback
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.sql.Date
import java.util.Calendar
import logics._
import org.specs2.mutable._
import org.specs2.matcher._
import org.specs2.matcher.EitherMatchers._
import org.specs2.specification.Scope
import play.api.test._
import play.api.test.Helpers._
import models._
import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import db.wrapper.specs2.AutoRollback
import models.DBWrap

class BookShelfLogicSpec extends Specification {
  val now = new Timestamp(Calendar.getInstance.getTimeInMillis)

  // 基本的なデータの追加・削除をするためのtrait
  trait OneData extends AutoRollback {
    override def fixture(implicit session: DBSession) = {
      val shelfId = BookShelves.ins.insert("book shelf", "description", now, 0L, now, 0L)
      val book = Books.ins.insert(0L, "book name", Some("book author"), Some("book isbn"),
        None, None, None, None, now, 0L, now, 0L)
      val user = UserInforms.ins.insert("", "", "", "", "", "", None, None, None, now, 0L, now, 0L)
        (for {s <- BookShelves if s.id === shelfId} yield (s.id)).update(0L)
        (for {s <- Books if s.bookId === book} yield (s.bookId)).update(0L)
        (for {s <- UserInforms if s.userId === user} yield (s.userId)).update(0L)
    }
  }

  "BookShelfLogicSpec" should {
    "updated updated-date of the bookshelf that is added a book" in {
      running(FakeApplication()) {
        new OneData {
          val newUser = 1L
          override def fixture(implicit session: DBSession) = {
            super.fixture(session)
            val cal = Calendar.getInstance
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val now = new Timestamp(cal.getTimeInMillis())
            (for {s <- BookShelves if s.id === 0L} yield (s.createdDate)).update(now)
            val user = UserInforms.ins.insert("new", "tmp", "", "", "", "", None, None, None, now, 1L, now, 1L)
            (for {s <- UserInforms if s.userId === user} yield (s.userId)).update(1L)
          }

          val shelfLogic = new BookShelfLogic
          val book_id = shelfLogic.addBook(0L,
            Register.Book("book name", None, None, None, None,
              None, Some("large image url"), 1L)
          )
          book_id must beRight

          val shelf = shelfLogic.getShelf(0L).get
          shelf._1.updated.getTime must beGreaterThan(shelf._1.created.getTime)
          val book = shelfLogic.getBook(book_id.right.get).get._1
          shelf._1.updated.getTime must be_==(book.created.getTime)
          shelf._2 must be_!=(shelf._3)
          shelf._3 must be_==("new")
        }
      }
    }
  }

  def addBook(shelfId: Long)(implicit ses:DBSession) : Long = 
    Books.ins.insert(shelfId, "book name", None, None,
      None, Some("large image url"), None, None, now, 0L, now, 0L)
}
