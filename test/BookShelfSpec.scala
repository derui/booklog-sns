package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.BookShelf

class BookShelfSpec extends Specification {

  "BookShelf" should  {
    "can insert and delete a book shelf information" in {
      running(FakeApplication()) {
        val result = BookShelf.insert("book shelf", "shelf description")
        result must beGreaterThan(0L)
        BookShelf.delete(result) must beEqualTo(1)
      }
    }
  }

}