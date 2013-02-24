package models

import play.api.Play.current
import play.api.db.DB
import anorm._

case class Shelf(id:BigInt, name:String)

object BookShelf {
  // 一件追加
  def insert(name:String):Any = {
    DB.withConnection { implicit connection =>
      SQL("insert into book_shelf (shelf_name) values ({name})").on("name" -> name).
      executeUpdate()
    }
  }

  def selectAll(): List[(Long, String)] = {
    DB.withConnection {implicit conn =>
      SQL("select * from book_shelf")().collect {
        case Row(id:Long, name:String) => (id, name)
      }.toList
    }
  }

  // 一件だけ取得
  def select(name:String): (Long, String) = {
    DB.withConnection {implicit conn =>
      SQL("select * from book_shelf where name = {name}").on("name" -> name)().collect {
        case Row(id:Long, name:String) => (id, name)
      }.toList.head
    }
  }
}
