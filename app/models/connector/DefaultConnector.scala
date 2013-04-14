package models.connector

import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

trait DefaultConnector extends Connector {
  override def getDB() : Database = new Database {
    override def createConnection(): Connection = DB.getConnection()
  }
}
