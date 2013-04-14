package models

import play.api.Logger
import scala.slick.driver.MySQLDriver.simple._

trait Logging {
  def log[G,T](q:Query[G,T]) {
    Logger.info("execute SQL : %s" format q.selectStatement)
  }

  def log(log:String) {
    Logger.info("execute SQL : %s" format log)
  }
}
