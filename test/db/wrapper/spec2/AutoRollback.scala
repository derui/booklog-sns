package db.wrapper.specs2

import org.specs2.mutable.After
import org.specs2.specification.Scope
import models.DBWrap
import scala.slick.driver.MySQLDriver.simple._

// traitの範囲を終了した時点で、行われたものをrollbackする。
trait AutoRollback extends Scope with After with DBWrap {
  implicit val session: Session = db.createSession()
  session.conn.setAutoCommit(false)

  fixture

  // 事前にデータの登録などを行いたい場合、このtraitを継承したtraitで
  // この関数をオーバーライドする。
  def fixture(implicit session: Session) {}

  // afterの処理は書き換え不可とする
  final override def after {
    session.rollback()
    session.close()
  }
}
