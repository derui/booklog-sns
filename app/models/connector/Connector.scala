package models.connector

import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps

// DBへの接続を行うためのSessionを提供する
trait Connector {
  def getDB() : Database
}
