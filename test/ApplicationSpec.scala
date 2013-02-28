package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.Book
import org.specs2.matcher._
import org.specs2.specification.Scope
import models.Book
import java.math.BigInteger
import models.BookShelf
import models.BookRegister
import models.BookDetail
import play.api.mvc.SimpleResult
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

class ApplicationSpec extends Specification {

  // 基本的なデータの追加・削除をするためのtrait
  trait scope extends Scope with After {
    val shelfId : BigInteger = BookShelf.insert("book shelf", "description")
    val result: BigInteger = Book.insert(BookRegister(shelfId, "book name", "book author", "book isbn")).right.get

    def after = {
      BookShelf.delete(shelfId)
      Book.delete(result)
    }
  }

  trait manyData extends Scope with After {
    val shelfId : BigInteger = BookShelf.insert("book shelf", "description")
    val results: List[BigInteger] = (1 to 10).map { e =>
      Book.insert(BookRegister(shelfId, "book name" + e.toString, "book author" + e.toString, e.toString)).right.get
    }.toList

    def after = {
      results.map(Book.delete)
      BookShelf.delete(shelfId)
    }
  }

  "Application" should {
    "makes an book shelf with name and description" in new WithApplication {
         val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
             "shelf_description" -> Seq("desc"))
        val result = route(FakeRequest(POST, "/shelf").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

        result must beSome
        status(result.get) must beEqualTo(OK)
        val node = Json.parse(contentAsString(result.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        (node \ "result" \ "id").as[Long] must be_>(1L)
    }
  }

}