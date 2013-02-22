pjackage models

import play.api.Play.current
import play.api.db.DB
import anorm._

object BookShelf {
  def insert(name:String):Any = {
    DB.withConnection { implicit connection =>
      SQL("insert into book_shelf values (0, {name})").on("name" -> name).apply()
    }
  }

  def select(name:String): (Long, String) = {
    DB.withConnection {implicit conn =>
      SQL("select * from book_shelf where name = {name}").on("name" -> name).apply() foreach {
        case Row (id: Long, name:String) => (id, name)
      }
    }
  }
}