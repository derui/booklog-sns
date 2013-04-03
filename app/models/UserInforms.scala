package models

import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import scala.language.postfixOps
import models.DBWrap._

// UserInfoテーブルの情報を表すcase class
case class UserInform(userId: Long,
                      userDisplayName: String,
                      userGoogleId: String,
                      userGoogleDisplayName: String,
                      userGooglePublicProfileUrl: String,
                      userGooglePublicPhotoUrl: String,
                      userGoogleAccessToken: String,
                      userGoogleRefreshToken: Option[String],
                      userGoogleExpiresAt: Option[Long],
                      userGoogleExpiresIn: Option[Long],
                      created: Timestamp, createdUser: Long, updated: Timestamp, updatedUser: Long)

// それぞれの本に対する操作を提供する
object UserInforms extends Table[UserInform]("user_info") with Logging {

  // 各カラム
  def userId = column[Long]("user_id", O.PrimaryKey, O AutoInc)

  def userDisplayName = column[String]("user_display_name", O DBType "varchar (200)")

  def userGoogleId = column[String]("user_google_id", O DBType "varchar (200)")

  def userGoogleDisplayName = column[String]("user_google_display_name", O DBType "varchar (200)")

  def userGooglePublicProfileUrl = column[String]("user_google_public_profile_url", O DBType "varchar (1000)")

  def userGooglePublicProfilePhotoUrl =
    column[String]("user_google_public_profile_photo_url", O DBType "varchar (1000)")

  def userGoogleAccessToken =
    column[String]("user_google_access_token", O DBType "varchar (300)")

  def userGoogleRefreshToken =
    column[Option[String]]("user_google_refresh_token", O DBType "varchar (300)")

  def userGoogleExpiresAt = column[Option[Long]]("user_google_expires_at")

  def userGoogleExpiresIn = column[Option[Long]]("user_google_expires_in")

  def createdDate = column[Timestamp]("created_date")

  def createdUser = column[Long]("created_user")

  def updatedDate = column[Timestamp]("updated_date")

  def updatedUser = column[Long]("updated_user")

  // UserInfoを構成する
  def * = userId ~ userDisplayName ~ userGoogleId ~ userGoogleDisplayName ~
    userGooglePublicProfileUrl ~ userGooglePublicProfilePhotoUrl ~ userGoogleAccessToken ~
    userGoogleRefreshToken ~ userGoogleExpiresAt ~ userGoogleExpiresIn ~
    createdDate ~ createdUser ~ updatedDate ~ updatedUser <>(UserInform, UserInform.unapply _)

  // UserInfoをinsertするためのメソッドinsを定義する
  def ins = userDisplayName ~ userGoogleId ~ userGoogleDisplayName ~
    userGooglePublicProfileUrl ~ userGooglePublicProfilePhotoUrl ~ userGoogleAccessToken ~
    userGoogleRefreshToken ~ userGoogleExpiresAt ~ userGoogleExpiresIn ~
    createdDate ~ createdUser ~ updatedDate ~ updatedUser returning userId

  // 内部ユーザーIDに一致する一件を取得する
  def selectById(id : Long)(implicit session : Session) : Option[UserInform] = {
    val query = for {
      u <- UserInforms
      if u.userId === id
    } yield u
    log(query)
    query.firstOption
  }

  def toJson(user: UserInform): JsValue = {
    implicit val dateWriter = Writes[Timestamp] {date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "user_id").write[Long] and
        (__ \ "user_display_name").write[String] and
        (__ \ "google_user_id").write[String] and
        (__ \ "google_display_name").write[String] and
        (__ \ "google_public_profile_url").write[String] and
        (__ \ "google_public_profile_photo_url").write[String] and
        (__ \ "google_access_token").write[String] and
        (__ \ "google_refresh_token").write[Option[String]] and
        (__ \ "google_expires_at").write[Option[Long]] and
        (__ \ "google_expires_in").write[Option[Long]] and
        (__ \ "created_date").write[Timestamp] and
        (__ \ "created_user").write[Long] and
        (__ \ "updated_date").write[Timestamp] and
        (__ \ "updated_user").write[Long])(unlift(UserInform.unapply))
    Json.toJson(user)
  }
}
