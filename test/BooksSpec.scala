package test

import anorm._
import java.util.Calendar
import java.sql.Date
import models._
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.matcher._
import org.specs2.specification.Scope
import scala.slick.driver.MySQLDriver.simple.{Session =>_, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import db.wrapper.specs2._

class BookSpec extends Specification {

  def now = new Date(Calendar.getInstance.getTimeInMillis)

  trait OneDataWithAutoRollback extends AutoRollback {
    override def fixture(implicit session:DBSession): Unit = {
      val shelfId = BookShelves.ins.insert("book shelf", "description", now, 0L, now, 0L)
      val bookId = Books.ins.insert(0L, "book name", Some("book author"), Some("book isbn"),
        None, Some("large image url"), Some("medium image url"), Some("small image url"),
        now, 0L, now, 0L)
      val userId = UserInforms.ins.insert("name", "gid", "guser", "gurl", "gphoto", "token", Some("refresh"),
        Some(0L), Some(0L), now, 0L, now, 0L)

      (for (r <- UserInforms if r.userId === userId) yield (r.userId)).update(0L)
      (for (r <- Books if r.bookId === bookId) yield (r.bookId)).update(0L)
      (for (r <- BookShelves if r.id === shelfId) yield (r.id)).update(0L)
    }
  }

  trait manyData extends AutoRollback {
    override def fixture(implicit session:DBSession): Unit = {
      val shelfId = BookShelves.ins.insert("book shelf", "description", now, 0L, now, 0L)

      val bookIds: List[Long] = (1 to 10).map { e =>
        Books.ins.insert(0L, "book name" + e.toString, Some("book author" + e.toString),
          Some(e.toString), None, Some("large image url"), Some("medium image url"), Some("small image url"),
          now, 0L, now, 0L)
      }.toList
      val userId = UserInforms.ins.insert("name", "gid", "guser", "gurl", "gphoto", "token", Some("refresh"),
        Some(0L), Some(0L), now, 0L, now, 0L)

      bookIds.zipWithIndex.foreach{case (bid, index) =>
          (for {b <- Books if b.bookId === bid} yield (b.bookId)).update(index)}
      (for (r <- UserInforms if r.userId === userId) yield (r.userId)).update(0L)
      (for (r <- BookShelves if r.id === shelfId) yield (r.id)).update(0L)
    }
  }

  "Book" should {
    "can insert and delete a book information in the book shelf" in {
      running(FakeApplication()) {
        new AutoRollback {
          val result = Books.ins.insert(0L, "book name", Some("author"), Some("isbn"),
            None, None, None, None, now, 0L, now, 0L)

          result must beGreaterThan(0L)
          Books.delete(result) must beEqualTo(1)
        }
      }
    }

    "can be selected a registered book in the book shelf by the ID" in {
      running(FakeApplication()) {
        new OneDataWithAutoRollback {
          val book = Books.selectById(0L)
          book must beSome
          val s = book.get._1
          s.name must beEqualTo("book name")
          s.author must beEqualTo(Some("book author"))
          s.isbn must beEqualTo(Some("book isbn"))
          s.publishedDate must beNone
          s.largeImageUrl must be_==(Some("large image url"))
          s.mediumImageUrl must be_==(Some("medium image url"))
          s.smallImageUrl must be_==(Some("small image url"))
          val jsoned = Books.toJson(s)
            (jsoned \ "created_date").as[String] must be_==("%tF %<tT" format s.created)
            (jsoned \ "updated_date").as[String] must be_==("%tF %<tT" format s.updated)
        }
      }
    }

    "can select many registered book shelf with limitation" in {
      running(FakeApplication()) {
        new manyData {
          {
            val books = Books.findAllInShelf(0L, None, None)
            books.size must beEqualTo(10)
          }

          {
            val books = Books.findAllInShelf(0L, Some(3), None)
            books.size must beEqualTo(7)
            books.map { case (s, _, _) => s.name.startsWith("book name") must beTrue }
          }

          {
            val books = Books.findAllInShelf(0L, None, Some(2))
            books.size must beEqualTo(2)
            books.map { case (s, _, _) => s.name.startsWith("book name") must beTrue }
          }

          {
            val books = Books.findAllInShelf(0L, Some(4), Some(2))
            books.size must beEqualTo(2)
            books.map { case (s, _, _) => s.name.startsWith("book name") must beTrue }
          }
        }
      }
    }
  }
}
