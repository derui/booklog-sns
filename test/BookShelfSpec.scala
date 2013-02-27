package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.BookShelf
import org.specs2.matcher._
import org.specs2.specification.Scope
import models.Shelf
import java.math.BigInteger

class BookShelfSpec extends Specification {

  // 基本的なデータの追加・削除をするためのtrait
  trait scope extends Scope with After {
    val result: BigInteger = BookShelf.insert("book shelf", "shelf description")

    def after = {
      println(result)
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
          println(result)
          val shelf = BookShelf.selectById(result)
          shelf must beSome { x: Shelf =>
            x.name must beEqualTo("book shelf")
            x.description must beEqualTo("shelf description")
          }
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
            shelfs.size must beEqualTo(7)
            shelfs.head.name must beEqualTo("book shelf3")
          }

          {
            val shelfs = BookShelf.all(None, Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.head.name must beEqualTo("book shelf0")
          }

          {
            val shelfs = BookShelf.all(Some(4), Some(2))
            shelfs.size must beEqualTo(2)
            shelfs.head.name must beEqualTo("book shelf5")
          }
        }
      }
    }
  }

}