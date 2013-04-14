package models

import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import java.sql.Connection
import play.api.Play.current
import models.connector._

// 内部で利用するためのラッパー
trait InnerWrapper {
  self: Connector =>
  val db : Database = this.getDB()
}

// DBについて、直接利用させずに、各環境毎に定義されたラッパーオブジェクトを
// 用いる。
trait DBWrap {
  val db = wrapper.ConnectionWrapper.db
}
