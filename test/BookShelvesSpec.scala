package test

import java.sql.Date
import java.util.Calendar
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models._
import org.specs2.matcher._
import org.specs2.specification._
import db.wrapper.specs2.AutoRollback
import scala.slick.driver.MySQLDriver.simple._

class BookShelvesSpec extends Specification {

  def now = new Date(Calendar.getInstance.getTimeInMillis)

  // 基本的なデータの追加・削除をするためのtrait
  trait OneDataAutoRollback extends AutoRollback {
    override def fixture(implicit session:Session): Unit = {
      val result = BookShelves.ins.insert("book shelf", "shelf description", now, 0L, now, 0L)
      val userId = UserInforms.ins.insert("name", "gid", "guser", "gurl", "gphoto", "token", Some("refresh"),
        Some(0L), Some(0L), now, 0L, now, 0L)

      (for (r <- UserInforms if r.userId === userId) yield (r.userId)).update(0L)
      (for {b <- BookShelves if b.id === result} yield b.id).update(0L)
    }
  }

  trait ManyDataAutoRollback extends AutoRollback {

    override def fixture(implicit session:Session): Unit = {
      val results: List[Long] = (1 to 10).map { e =>
        BookShelves.ins.insert("book shelf", "shelf description", now, 0L, now, 0L)
      }.toList
      results.zipWithIndex.foreach {case (r : Long, index: Int) =>
        (for {b <- BookShelves if b.id === r} yield b.id).update(index)
      }
      val userId = UserInforms.ins.insert("name", "gid", "guser", "gurl", "gphoto", "token", Some("refresh"),
        Some(0L), Some(0L), now, 0L, now, 0L)

      (for (r <- UserInforms if r.userId === userId) yield (r.userId)).update(0L)
    }
  }

  "BookShelf" should {
    "can insert and delete a book shelf information" in {
      running(FakeApplication()) {
        new AutoRollback {
          val result = BookShelves.ins.insert("book shelf", "shelf description", now, 0L, now, 0L)
          result must beGreaterThan(0L)
          BookShelves.delete(result) must beEqualTo(1)
        }
      }
    }

    "can be selected a registered book shelfs by the ID" in {
      running(FakeApplication()) {
        new OneDataAutoRollback {
          val shelf= BookShelves.selectById(0L)
          shelf must beSome
          val s = shelf.get._1
          s.name must beEqualTo("book shelf")
          s.description must beEqualTo("shelf description")
          val jsoned = BookShelves.toJson(s)
          (jsoned \ "created_date").as[String] must be_==("%tF %<tT" format s.created)
          (jsoned \ "updated_date").as[String] must be_==("%tF %<tT" format s.updated)
        }
      }
    }

    "can select many registered book shelf with limitation" in {
      running(FakeApplication()) {
        new ManyDataAutoRollback {
          {
            val shelfs = BookShelves.findAll(None, None)
            shelfs.size must beEqualTo(10)
          }

          {
            val shelfs = BookShelves.findAll(Some(3), None)
            shelfs.size must beEqualTo(7)
            shelfs.map {case (s,_, _) => s.name.startsWith("book shelf") must beTrue }
          }

          {
            val shelfs = BookShelves.findAll(None, Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.map { case (s,_,_) => s.name.startsWith("book shelf") must beTrue }
          }

          {
            val shelfs = BookShelves.findAll(Some(4), Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.map { case (s, _,_) => s.name.startsWith("book shelf") must beTrue }
          }
        }
      }
    }
  }

}
