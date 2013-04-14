package test

import db.wrapper.specs2.AutoRollback
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.sql.Date
import java.util.Calendar
import org.specs2.mutable._
import org.specs2.specification._
import org.specs2.matcher._
import org.specs2.specification.Scope
import play.api.test._
import play.api.test.Helpers._
import models._
import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import db.wrapper.specs2.AutoRollback
import models.DBWrap

class ApplicationSpec extends Specification {
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

  "Application" should {
    "makes and delete an book shelf with name and description" in new WithApplication {
      new AutoRollback {
        val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
          "shelf_description" -> Seq("desc"))
        val result = route(FakeRequest(POST, "/api/shelf").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

        result must beSome
        status(result.get) must beEqualTo(OK)
        val node = Json.parse(contentAsString(result.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "id").as[Long] must be_>=(1L)
        val id = ((node \ "result")(0) \ "id").as[Long]

        val resultByDelete = route(FakeRequest(DELETE, "/api/shelf/" + id.toString))
        resultByDelete must beSome
        status(resultByDelete.get) must beEqualTo(OK)
      }
    }

    "return BadRequest when give id is not found in data store" in new WithApplication {

      var result = route(FakeRequest(DELETE, "/api/shelf/0"))
      result must beSome
      status(result.get) must beEqualTo(400)
    }

    "get information of a book shelf if it is registered" in new WithApplication {
      new OneData {

        val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
          "shelf_description" -> Seq("desc"))
        val result = route(FakeRequest(POST, "/api/shelf").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

        result must beSome
        val id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]
        id must be_>(0L)
        val shelf = route(FakeRequest(GET, "/api/shelf/" + id))

        shelf must beSome
        status(shelf.get) must beEqualTo(OK)
        val node = Json.parse(contentAsString(shelf.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "shelf_id").as[Long] must be_==(id)
        ((node \ "result")(0) \ "shelf_name").as[String] must be_==("name")
        ((node \ "result")(0) \ "shelf_description").as[String] must be_==("desc")

        val deleted = route(FakeRequest(DELETE, "/api/shelf/" + id))
        deleted must beSome
        status(deleted.get) must beEqualTo(OK)
        (Json.parse(contentAsString(deleted.get)) \ "totalCount").as[Long] must be_==(0L)
      }
    }

    "makes and delete book information with" in new WithApplication {
      new OneData {

        val shelf: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
          "shelf_description" -> Seq("desc"))
        val result = route(FakeRequest(POST, "/api/shelf").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), shelf)

        result must beSome
        val shelf_id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]

        val book_req: Map[String, Seq[String]] = Map("shelf_id" -> Seq(shelf_id.toString),
          "book_name" -> Seq("book name"),
          "published_date" -> Seq("2012/02/01"),
          "large_image_url" -> Seq("large image url"))
        val book_result = route(FakeRequest(POST, "/api/book").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), book_req)

        book_result must beSome
        status(book_result.get) must beEqualTo(OK)

        val book_id = ((Json.parse(contentAsString(book_result.get)) \ "result")(0) \ "id").as[Long]
        val book = route(FakeRequest(GET, "/api/book/" + book_id))

        book must beSome
        val node = Json.parse(contentAsString(book.get))
        val formatter = new SimpleDateFormat("yyyy/MM/dd")
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "book_id").as[Long] must be_==(book_id)
        ((node \ "result")(0) \ "book_name").as[String] must be_==("book name")
        ((node \ "result")(0) \ "book_author").asOpt[String] must beNone
        ((node \ "result")(0) \ "book_isbn").asOpt[String] must beNone
        ((node \ "result")(0) \ "published_date").as[Date] must be_==("2012/02/01") ^^ ("%tY/%<tm/%<td" format (_: Date))
        ((node \ "result")(0) \ "large_image_url").as[String] must be_==("large image url")
        ((node \ "result")(0) \ "medium_image_url").asOpt[String] must beNone
        ((node \ "result")(0) \ "small_image_url").asOpt[String] must beNone

        val deleted = route(FakeRequest(DELETE, "/api/shelf/" + shelf_id))
        deleted must beSome
        status(deleted.get) must beEqualTo(OK)
        (Json.parse(contentAsString(deleted.get)) \ "totalCount").as[Long] must be_==(0L)
      }
    }

    "update and get user information" in new WithApplication {
      new OneData {

        val userInfo: Map[String, Seq[String]] = Map("user_display_name" -> Seq("name"))
        val result = route(FakeRequest(PUT, "/api/login_user_info").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), userInfo)

        result must beSome
        status(result.get) must beEqualTo(OK)

        val updated = route(FakeRequest(GET, "/api/login_user_info"))

        updated must beSome
        status(updated.get) must beEqualTo(OK)
        val node = Json.parse(contentAsString(updated.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "user_id").as[Long] must be_==(0L)
        ((node \ "result")(0) \ "user_display_name").as[String] must be_==("name")
        ((node \ "result")(0) \ "google_user_id").as[String] must be_==("")
        ((node \ "result")(0) \ "google_display_name").as[String] must be_==("")
        ((node \ "result")(0) \ "google_public_profile_url").as[String] must be_==("")
        ((node \ "result")(0) \ "google_public_profile_photo_url").as[String] must be_==("")
      }
    }

    "detect environment" in new WithApplication {
      val env = route(FakeRequest(GET, "/api/detect_env"))
      env must beSome
      status(env.get) must beEqualTo(OK)

      val node = Json.parse(contentAsString(env.get))
      (node \ "environment").as[String] must be_==("develop")
    }

    "modify updated date of the bookshelf that is added a book" in new WithApplication {
      new OneData {
        override def fixture(implicit session: DBSession) = {
          super.fixture(session)
          val cal = Calendar.getInstance
          cal.add(Calendar.DAY_OF_YEAR, -2)
          val now = new Timestamp(cal.getTimeInMillis())
          (for {s <- BookShelves if s.id === 0L} yield (s.createdDate)).update(now)
        }

        val book_id = addBook(0L)
        val book = route(FakeRequest(GET, "/api/book/" + book_id))

        book must beSome
        val node = Json.parse(contentAsString(book.get))
        val formatter = new SimpleDateFormat("yyyy/MM/dd")
        val bookRegistered = ((node \ "result")(0) \ "created_date").as[Date]

        val shelf = route(FakeRequest(GET, "/api/shelf/" + 0L))
        val shelf_node = Json.parse(contentAsString(shelf.get))
        val shelfUpdated = ((shelf_node \ "result")(0) \ "updated_date").as[Date]
        val shelfRegistered = ((shelf_node \ "result")(0) \ "created_date").as[Date]

        bookRegistered.getTime must be_==(shelfUpdated.getTime)
        shelfUpdated.getTime must beGreaterThan(shelfRegistered.getTime)
      }
    }
  }

  def addBook(shelfId: Long)(implicit application:FakeApplication) = {
    val book_req: Map[String, Seq[String]] = Map("shelf_id" -> Seq(shelfId.toString),
      "book_name" -> Seq("book name"),
      "published_date" -> Seq("2012/02/01"),
      "large_image_url" -> Seq("large image url"))
    val book_result = route(FakeRequest(POST, "/api/book").withHeaders(
      CONTENT_TYPE -> "application/x-www-form-urlencode"), book_req)

    book_result must beSome
    status(book_result.get) must beEqualTo(OK)

    ((Json.parse(contentAsString(book_result.get)) \ "result")(0) \ "id").as[Long]
  }
}
