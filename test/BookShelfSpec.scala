package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.BookShelf
import org.specs2.matcher._
import org.specs2.specification.Scope

class BookShelfSpec extends Specification {

  // 基本的なデータの追加・削除をするためのtrait
  trait scope extends Scope with After {
    val result :Long = BookShelf.insert("book shelf", "shelf description")

    def after = {
      BookShelf.delete(result)
    }
  }

  trait manyData extends Scope with After {
    val results : List[Long] = (1 to 10).map { e =>
        BookShelf.insert("book shelf" + e.toString, "shelf description" + e.toString)
    }

    def after = {
      results.map(BookShelf.delete)
    }

  }

  "BookShelf"should  {
    "can insert and delete a book shelf information" in {
      running(FakeApplication()) {
        val result = BookShelf.insert("book shelf", "shelf description")
        result must beGreaterThan(0L)
        BookShelf.delete(result) must beEqualTo(1)
      }
    }

    "can be selected a registered book shelfs by the ID" in {
      running(FakeApplication()) in new scope{
        val shelf = BookShelf.selectById(id)
        shelf.name must beEqualTo("book shelf")
        shelf.description must beEqualTo("shelf description")
      }
    }

    "can select many registered book shelf with limitation" in {
      running(FakeApplication()) in new manyData {
        val shelfs : List[Shelf] = BookShelf.all(None, None)
        shelfs.size must beEqualTo(10)

        val shelfs = BookShelf.all(Some(3), None)
        shelfs.size must beEqualTo(7)
        shelfs.head.name must beEqualTo("book shelf3")

        val shelfs = BookShelf.all(None, Some(2))
        shelfs.size must beEqualTo(2)
        shelfs.head.name must beEqualTo("book shelf0")

        val shelfs = BookShelf.all(Some(4), Some(2))
        shelfs.size must beEqualTo(2)
        shelfs.head.name must beEqualTo("book shelf5")
      }
    }
  }

}