package test

import java.sql.Date
import java.util.Calendar
import models._
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import models.DBWrap.UsePerDB

class RentalSpec extends Specification {

  val now = new Date(Calendar.getInstance.getTimeInMillis)

  trait OneData extends Scope with After with UsePerDB {
    db.withSession {
      implicit session: DBSession =>
        (for {s <- Books} yield s).delete
        (for {s <- UserInforms} yield s).delete
        (for {s <- RentalInforms} yield s).delete

        val book = Books.ins.insert(0L, "", None, None, None, None, None, None, now, 0L, now, 0L)
        (for {b <- Books if b.bookId === book} yield (b.bookId)).update(0L)

        val user = UserInforms.ins.insert("", "", "", "", "", "", None, None, None, now, 0L, now, 0L)
        (for {
          u <- UserInforms
          if u.userId === user
        } yield (u.userId)).update(0L)
    }

    override def after {
      db.withSession {
        implicit session: DBSession =>
          (for {s <- Books} yield s).delete
          (for {s <- UserInforms} yield s).delete
          (for {s <- RentalInforms} yield s).delete
      }
    }
  }

  "Rental" should {
    "be able to rental book" in new WithApplication {
      new OneData {
        val req: Map[String, Seq[String]] = Map("rental_book" -> Seq("0"))
        val result = route(FakeRequest(POST, "/api/rental").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), req)

        result must beSome
        status(result.get) must beEqualTo(OK)

        val rentalId: Long =
          ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "rental_id").as[Long]

        val rentaled = route(FakeRequest(GET, "/api/rental/%d" format rentalId))

        rentaled must beSome
        val info = Json.parse(contentAsString(rentaled.get))
        (info \ "totalCount").as[Long] must be_==(1L)
        ((info \ "result")(0) \ "rental_user_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_book_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_now").as[Boolean] must beTrue
      }
    }

    "be able to get rental information by book id" in new WithApplication {
      new OneData {

        val req: Map[String, Seq[String]] = Map("rental_book" -> Seq("0"))
        val posted = route(FakeRequest(POST, "/api/rental").withHeaders(
          CONTENT_TYPE -> "application/x-www-form-urlencode"), req)

        posted must beSome
        status(posted.get) must beEqualTo(OK)

        val result = route(FakeRequest(GET, "/api/rental/?book_id=0"))

        result must beSome
        status(result.get) must beEqualTo(OK)

        val info = Json.parse(contentAsString(result.get))
        (info \ "totalCount").as[Long] must be_==(1L)
        ((info \ "result")(0) \ "rental_user_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_book_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_now").as[Boolean] must beTrue
      }
    }
  }
}
