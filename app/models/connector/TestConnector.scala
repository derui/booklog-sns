package models.connector

import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

// テスト環境におけるConnectorを指定する。
// テスト環境では、常に同一のConnectionで処理を行う。
trait TestConnector extends Connector {
  private var con = DB.getConnection()

  con.setAutoCommit(false)
  
  override def getDB() : Database = new Database {
    override def createConnection(): Connection = {
      if (con.isClosed()) {
        con = DB.getConnection()
        con.setAutoCommit(false)
        con
      } else {
        con
      }
    }

    override def withSession[T](f: Session => T) :T = f(createSession)
    override def withSession[T](f: => T) :T = f

    override def withTransaction[T](f:Session => T): T = {
      val ses = createSession
      ses.conn.setAutoCommit(false)
      val r = f(ses)
      r
    }
    override def withTransaction[T](f: => T): T = f
  }
}
