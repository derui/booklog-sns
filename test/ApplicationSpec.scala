package test

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.Date
import models.UserInfo
import org.specs2.mutable._
import play.api.db._
import anorm._
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
    val shelfId: BigInteger = BookShelf.insert("book shelf", "description", BigInteger.valueOf(0L))
    val result: BigInteger = Book.insert(BookRegister(shelfId, "book name",
      Some("book author"), Some("book isbn"),
      None,None, None, None), BigInteger.valueOf(0L)).right.get

    def after = {
      BookShelf.delete(shelfId)
      Book.delete(result)
    }
  }

  trait manyData extends Scope with After {
    val shelfId: BigInteger = BookShelf.insert("book shelf", "description", BigInteger.valueOf(0L))
    val results: List[BigInteger] = (1 to 10).map { e =>
      Book.insert(BookRegister(shelfId, "book name" + e.toString, Some("book author" + e.toString),
        Some(e.toString),None, None, None, None),
      BigInteger.valueOf(0L)).right.get
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
      val result = route(FakeRequest(POST, "/api/shelf").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

      result must beSome
      status(result.get) must beEqualTo(OK)
      val node = Json.parse(contentAsString(result.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "id").as[Long] must be_>(1L)
      val id = ((node \ "result")(0) \ "id").as[Long]

      val resultByDelete = route(FakeRequest(DELETE, "/api/shelf/" + id.toString))
      resultByDelete must beSome
      status(resultByDelete.get) must beEqualTo(OK)
    }

    "return BadRequest when give id is not found in data store" in new WithApplication {

      var result = route(FakeRequest(DELETE, "/api/shelf/0"))
      result must beSome
      status(result.get) must beEqualTo(400)
    }

    "get infomation of a book shelf if it is registered" in new WithApplication {

      DB.withConnection {implicit conn:Connection =>
        SQL(
          """
          insert into UserInfo values (0, 'name', 'gid', 'guser', 'gurl', 'gphoto', 'token', 'refresh', 0, 0, '2012-01-01 00:00:00', '',
          '2012-01-01 00:00:00', '')
          """).executeUpdate
        val user = SQL("select last_insert_id() as lastnum from UserInfo").apply.head[BigInteger]("lastnum")
        SQL(
          """
          update UserInfo set user_id = 0 where user_id = {id}
          """).on('id -> user).executeUpdate
      }

      val data: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
        "shelf_description" -> Seq("desc"))
      val result = route(FakeRequest(POST, "/api/shelf").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"), data)

      result must beSome
      val id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]
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

      DB.withConnection {implicit conn => SQL("delete from UserInfo").executeUpdate}
    }

    "makes and delete book information with" in new WithApplication {

      DB.withConnection {implicit conn:Connection =>
        SQL(
          """
          insert into UserInfo values (0, 'name', 'gid', 'guser', 'gurl', 'gphoto', 'token', 'refresh', 0, 0, '2012-01-01 00:00:00', '',
          '2012-01-01 00:00:00', '')
          """).executeUpdate
        val user = SQL("select last_insert_id() as lastnum from UserInfo").apply.head[BigInteger]("lastnum")
        SQL(
          """
          update UserInfo set user_id = 0 where user_id = {id}
          """).on('id -> user).executeUpdate
      }

      val shelf: Map[String, Seq[String]] = Map("shelf_name" -> Seq("name"),
        "shelf_description" -> Seq("desc"))
      val result = route(FakeRequest(POST, "/api/shelf").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"), shelf)

      result must beSome
      val shelf_id = ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "id").as[Long]

      val book_req : Map[String, Seq[String]]= Map("shelf_id" -> Seq(shelf_id.toString),
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
        ((node \ "result")(0) \ "published_date").as[Date] must be_==("2012/02/01") ^^ ("%tY/%<tm/%<td" format (_:Date))
        ((node \ "result")(0) \ "large_image_url").as[String] must be_==("large image url")
        ((node \ "result")(0) \ "medium_image_url").asOpt[String] must beNone
        ((node \ "result")(0) \ "small_image_url").asOpt[String] must beNone

      val deleted = route(FakeRequest(DELETE, "/api/shelf/" + shelf_id))
      deleted must beSome
      status(deleted.get) must beEqualTo(OK)
        (Json.parse(contentAsString(deleted.get)) \ "totalCount").as[Long] must be_==(0L)

      DB.withConnection {implicit conn => SQL("""delete from UserInfo""").executeUpdate}
    }

    "update user infomation" in new WithApplication {
      DB.withConnection {implicit conn =>
        SQL(
          """
          insert into UserInfo values (0, '', '', '', '', '', '', '', 0, 0, '2012-01-01 00:00:00', '',
          '2012-01-01 00:00:00', '')
          """).executeUpdate
        val id = SQL("select last_insert_id() as lastnum from UserInfo").apply.head[BigInteger]("lastnum")
        SQL(
          """
          update UserInfo set user_id = 0 where user_id = {id}
          """).on('id -> id).executeUpdate
      }

      val userinfo: Map[String, Seq[String]] = Map("user_display_name" -> Seq("name"))
      val result = route(FakeRequest(PUT, "/api/user_info").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"), userinfo)

      result must beSome
      status(result.get) must beEqualTo(OK)

      val user = UserInfo.selectById(BigInteger.valueOf(0L))
      user must beSome
      user.get.userDisplayName must be_==("name")

      DB.withConnection {implicit conn =>
        SQL("""delete from UserInfo""").executeUpdate
      }
    }

    "get user infomation" in new WithApplication {
      DB.withConnection {implicit conn =>
        SQL(
          """
          insert into UserInfo values (0, 'name', 'gid', 'guser', 'gurl', 'gphoto', 'token', 'refresh', 0, 0, '2012-01-01 00:00:00', '',
          '2012-01-01 00:00:00', '')
          """).executeUpdate
        val id = SQL("select last_insert_id() as lastnum from UserInfo").apply.head[BigInteger]("lastnum")
        SQL(
          """
          update UserInfo set user_id = 0 where user_id = {id}
          """).on('id -> id).executeUpdate
      }

      val result = route(FakeRequest(GET, "/api/user_info").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"))

      result must beSome
      status(result.get) must beEqualTo(OK)

      val node = Json.parse(contentAsString(result.get))
        (node \ "totalCount").as[Long] must be_==(1L)
        ((node \ "result")(0) \ "user_id").as[Long] must be_==(0L)
        ((node \ "result")(0) \ "user_display_name").as[String] must be_==("name")
        ((node \ "result")(0) \ "google_user_id").as[String] must be_==("gid")
        ((node \ "result")(0) \ "google_display_name").as[String] must be_==("guser")
        ((node \ "result")(0) \ "google_public_profile_url").as[String] must be_==("gurl")
        ((node \ "result")(0) \ "google_public_profile_photo_url").as[String] must be_==("gphoto")

      DB.withConnection {implicit conn =>
        SQL("""delete from UserInfo""").executeUpdate
      }
    }

  }
}
