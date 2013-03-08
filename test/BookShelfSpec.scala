package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.BookShelf
import org.specs2.matcher._
import org.specs2.specification.Scope
import models.Shelf
import java.math.BigInteger
import models.BookShelf

class BookShelfSpec extends Specification {

  // 基本的なデータの追加・削除をするためのtrait
  trait scope extends Scope with After {
    val result: BigInteger = BookShelf.insert("book shelf", "shelf description")

    def after = {
      BookShelf.delete(result)
    }
  }

  trait manyData extends Scope with After {
    val results: List[BigInteger] = (1 to 10).map { e =>
      BookShelf.insert("book shelf" + e.toString, "shelf description" + e.toString)
    }.toList

    def after = {
      results.map(BookShelf.delete)
    }

  }

  "BookShelf" should {
    "can insert and delete a book shelf information" in {
      running(FakeApplication()) {
        val result = BookShelf.insert("book shelf", "shelf description")
        result must beGreaterThan(BigInteger.valueOf(0L))
        BookShelf.delete(result) must beEqualTo(1)
      }
    }

    "can be selected a registered book shelfs by the ID" in {
      running(FakeApplication()) {
        new scope {
          val shelf: Option[Shelf] = BookShelf.selectById(result)
          shelf must beSome
          val s = shelf.get
          s.name must beEqualTo("book shelf")
          s.description must beEqualTo("shelf description")
          val jsoned = BookShelf.toJson(s)
          (jsoned \ "created_date").as[String] must be_==("%tF %<tT" format s.created)
          (jsoned \ "updated_date").as[String] must be_==("%tF %<tT" format s.updated)
        }
      }
    }

    "can select many registered book shelf with limitation" in {
      running(FakeApplication()) {
        new manyData {
          {
            val shelfs: List[Shelf] = BookShelf.all(None, None)
            shelfs.size must beEqualTo(10)
          }

          {
            val shelfs = BookShelf.all(Some(3), None)
            shelfs.size must beEqualTo(10)
            shelfs.map { s => s.name.startsWith("book shelf") must beTrue }
          }

          {
            val shelfs = BookShelf.all(None, Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.map { s => s.name.startsWith("book shelf") must beTrue }
          }

          {
            val shelfs = BookShelf.all(Some(4), Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.map { s => s.name.startsWith("book shelf") must beTrue }
          }
        }
      }
    }
  }

}
