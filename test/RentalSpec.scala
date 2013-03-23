package test

import anorm._
import java.math.BigInteger
import models.RentalInfo
import models.UserInfo
import org.specs2.matcher._
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.db._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.functional.syntax._

class RentalSpec extends Specification {

  "Rental" should {
    "be able to rental book" in new WithApplication {
      DB.withConnection {implicit conn =>
        SQL(
          """
          insert into book values (0, 0, '', '', '', '2012-01-01 00:00:00', '', '', '', '2012-01-01 00:00:00', '',
          '2012-01-01 00:00:00', '')
          """).executeUpdate
        val id = SQL("select last_insert_id() as lastnum from book").apply.head[BigInteger]("lastnum")
        SQL(
          """
          update book set book_id = 0 where book_id = {id}
          """).on('id -> id).executeUpdate
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

      val req: Map[String, Seq[String]] = Map("rental_book" -> Seq("0"))
      val result = route(FakeRequest(POST, "/api/rental").withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"), req)

      result must beSome
      status(result.get) must beEqualTo(OK)

      val rentalId : Long =
        ((Json.parse(contentAsString(result.get)) \ "result")(0) \ "rental_id").as[Long]

      val rentaled = route(FakeRequest(GET, "/api/rental/%d" format rentalId).withHeaders(
        CONTENT_TYPE -> "application/x-www-form-urlencode"))

      rentaled must beSome
      val info = Json.parse(contentAsString(rentaled.get))
      (info \ "totalCount").as[Long] must be_==(1L)
      ((info \ "result")(0) \ "rental_user_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_book_id").as[Long] must be_==(0L)
        ((info \ "result")(0) \ "rental_now").as[Boolean] must beTrue

      DB.withConnection {implicit conn =>
        SQL("""delete from RentalInfo""").executeUpdate
        SQL("""delete from book""").executeUpdate
        SQL("""delete from UserInfo""").executeUpdate
      }
    }
  }
}
