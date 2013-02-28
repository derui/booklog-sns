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

class BookSpec extends Specification {

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

  "Book" should {
    "can insert and delete a book information in the book shelf" in {
      running(FakeApplication()) {
        val shelfId = BookShelf.insert("book shelf", "description")
        val result = Book.insert(BookRegister(shelfId, "book name", "author", "isbn"))
        result must beAnInstanceOf[Either[String, BigInteger]]
        val id = result.right.get
        id must beGreaterThan(BigInteger.valueOf(0L))
        Book.delete(id) must beEqualTo(1)
        BookShelf.delete(shelfId) must beEqualTo(1)
      }
    }

    "can be selected a registered book in the book shelf by the ID" in {
      running(FakeApplication()) {
        new scope {
          val shelf: Option[BookDetail] = Book.selectById(result)
          shelf must beSome
          val s = shelf.get
          s.name must beEqualTo("book name")
          s.author must beEqualTo("book author")
          s.isbn must beEqualTo("book isbn")
        }
      }
    }

    "can select many registered book shelf with limitation" in {
      running(FakeApplication()) {
        new manyData {
          {
            val books: List[BookDetail] = Book.allInShelf(shelfId, None, None)
            books.size must beEqualTo(10)
          }

          {
            val books = Book.allInShelf(shelfId, Some(3), None)
            books.size must beEqualTo(10)
            books.map { s => s.name.startsWith("book name") must beTrue }
          }

          {
            val books = Book.allInShelf(shelfId, None, Some(2))
            books.size must beEqualTo(2)
            books.map { s => s.name.startsWith("book name") must beTrue }
          }

          {
            val books = Book.allInShelf(shelfId, Some(4), Some(2))
            books.size must beEqualTo(2)
            books.map { s => s.name.startsWith("book name") must beTrue }
          }
        }
      }
    }
  }

}