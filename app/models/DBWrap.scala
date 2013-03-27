package models

import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import java.sql.Connection
import play.api.db.DB
import play.api.Play.current

object DBWrap {
  // Playが提供するDBPoolを利用したDBを利用する場合にmixinするtrait
  trait UsePerDB {
    val db = new Database {
      override def createConnection(): Connection = DB.getConnection()
    }
  }
}
