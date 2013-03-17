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
import views.html.defaultpages.badRequest
import models.BookShelf
import models.Book

class ApplicationSpec extends Specification {

  // 基本的なデータの追加・削除をするためのtrait
  trait scope extends Scope with After {
    val shelfId: BigInteger = BookShelf.insert("book shelf", "description")
    val result: BigInteger = Book.insert(BookRegister(shelfId, "book name", "book author", "book isbn",
                                                      "", "", ""
                                                    )).right.get

    def after = {
      BookShelf.delete(shelfId)
      Book.delete(result)
    }
  }

  trait manyData extends Scope with After {
    val shelfId: BigInteger = BookShelf.insert("book shelf", "description")
    val results: List[BigInteger] = (1 to 10).map { e =>
      Book.insert(BookRegister(shelfId, "book name" + e.toString, "book author" + e.toString, e.toString,
                             "", "", "")).right.get
    }.toList

    def after = {
      results.map(Book.delete)
      BookShelf.delete(shelfId)
    }
  }

  "Application" should {
    "makes and delete an book shelf with name and description" in new WithApplication {
      val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
        "shelf_description" -> Seq("desc"))
      val result = route(FakeRequest(POST, "/shelf").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

      result must beSome
      status(result.get) must beEqualTo(OK)
      val node = Json.parse(contentAsString(result.get))
      (node \ "totalCount").as[Long] must be_==(1L)
      ((node \ "result")(0) \ "id").as[Long] must be_>(1L)
      val id = ((node \ "result")(0) \ "id").as[Long]

      val resultByDelete = route(FakeRequest(DELETE, "/shelf/" + id.toString))
      resultByDelete must beSome
      status(resultByDelete.get) must beEqualTo(OK)
    }

    "return BadRequest when give id is not found in data store" in new WithApplication {

      var result = route(FakeRequest(DELETE, "/shelf/0"))
      result must beSome
      status(result.get) must beEqualTo(400)
    }

    "get infomation of a book shelf if it is registered" in new WithApplication {
      val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
        "shelf_description" -> Seq("desc"))
      val result = route(FakeRequest(POST, "/shelf").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

      result must beSome
      val id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]
      val shelf = route(FakeRequest(GET, "/shelf/" + id))

      shelf must beSome
      status(shelf.get) must beEqualTo(OK)
      val node = Json.parse(contentAsString(shelf.get))
      (node \ "totalCount").as[Long] must be_==(1L)
      ((node \ "result")(0) \ "shelf_id").as[Long] must be_==(id)
      ((node \ "result")(0) \ "shelf_name").as[String] must be_==("name")
      ((node \ "result")(0) \ "shelf_description").as[String] must be_==("desc")

      val deleted = route(FakeRequest(DELETE, "/shelf/" + id))
      deleted must beSome
      status(deleted.get) must beEqualTo(OK)
      (Json.parse(contentAsString(deleted.get)) \ "totalCount").as[Long] must be_==(0L)
    }

    "makes and delete book information with" in new WithApplication {
      val shelf: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
        "shelf_description" -> Seq("desc"))
      val result = route(FakeRequest(POST, "/shelf").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencode"), shelf)

      result must beSome
      val shelf_id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]

      val book_req : Map[String, Seq[String]]= Map("shelf_id" -> Seq(shelf_id.toString),
                                                   "book_name" -> Seq("book name"),
                                                   "book_author" -> Seq(""),
                                                   "book_isbn" -> Seq(""),
                                                   "medium_image_url" -> Seq(""),
                                                   "small_image_url" -> Seq(""),
                                                   "large_image_url" -> Seq("large image url"))
      val book_result = route(FakeRequest(POST, "/book").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"), book_req)

      book_result must beSome
      status(book_result.get) must beEqualTo(OK)

      val book_id = ((Json.parse(contentAsString(book_result.get)) \ "result")(0) \ "id").as[Long]
      val book = route(FakeRequest(GET, "/book/" + book_id))

      book must beSome
      val node = Json.parse(contentAsString(book.get))
      (node \ "totalCount").as[Long] must be_==(1L)
      ((node \ "result")(0) \ "book_id").as[Long] must be_==(book_id)
      ((node \ "result")(0) \ "book_name").as[String] must be_==("book name")
      ((node \ "result")(0) \ "book_author").as[String] must be_==("")
      ((node \ "result")(0) \ "book_isbn").as[String] must be_==("")
      ((node \ "result")(0) \ "large_image_url").as[String] must be_==("large image url")
      ((node \ "result")(0) \ "medium_image_url").as[String] must be_==("")
      ((node \ "result")(0) \ "small_image_url").as[String] must be_==("")

      val deleted = route(FakeRequest(DELETE, "/shelf/" + shelf_id))
      deleted must beSome
      status(deleted.get) must beEqualTo(OK)
      (Json.parse(contentAsString(deleted.get)) \ "totalCount").as[Long] must be_==(0L)
    }

  }

}
