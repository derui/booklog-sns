package models

import java.util.Calendar
import java.util.Date
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.math.BigInteger

// UserInfoテーブルの情報を表すcase class
case class UserInfo(userId: BigInteger,
  userDisplayName: String,
  userGoogleId: String,
  userGoogleDisplayName: String,
  userGooglePublicProfileUrl: String,
  userGooglePublicPhotoUrl : String,
  userGoogleAccessToken: String,
  userGoogleRefreshToken: String,
  userGoogleExpiresAt : Long,
  userGoogleExpiresIn : Long,
  created: Date, cuser: String, updated: Date, uuser: String)

// UserInfoテーブルへ登録する際に利用するcase class
case class UserInfoRegister(userGoogleId:String,
  userGoogleDisplayName :String,
  userGooglePublicProfileUrl : String,
  userGooglePublicPhotoUrl : String,
  userGoogleAccessToken: String,
  userGoogleRefreshToken:String,
  userGoogleExpiresAt:Long,
  userGoogleExpiresIn : Long
)

// それぞれの本に対する操作を提供する
object UserInfo {
  private val tableName = "UserInfo"

  val userInfo = {
    get[BigInteger]("user_id") ~
    get[String]("user_display_name") ~
    get[String]("user_google_id") ~
    get[String]("user_google_display_name") ~
    get[String]("user_google_public_profile_url") ~
    get[String]("user_google_public_profile_photo_url") ~
    get[String]("user_google_access_token") ~
    get[String]("user_google_refresh_token") ~
    get[Long]("user_google_expires_at") ~
    get[Long]("user_google_expires_in") ~
    get[Date]("created_date") ~
    get[String]("created_user") ~
    get[Date]("updated_date") ~
    get[String]("updated_user") map {
      case id ~ uname ~ google_id ~ name ~ url ~ photo ~ access ~ refresh ~ expireat ~ expirein ~
          created ~ cuser ~ updated ~ uuser =>
        UserInfo(id, uname, google_id, name, url, photo, access, refresh, expireat, expirein,
          created, cuser, updated, uuser)
    }
  }

  // 一件追加
  def insert(user: UserInfoRegister): Either[String, BigInteger] = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit conn =>
      SQL("""
          insert into %s (user_display_name, user_google_id, user_google_display_name,
          user_google_public_profile_url, user_google_public_profile_photo_url,
          user_google_access_token, user_google_refresh_token,
          user_google_expires_at, user_google_expires_in,
          created_date, created_user, updated_date, updated_user)
          values ({user_display_name}, {id}, {name}, {url}, {photo}, {access}, {refresh},
                  {at}, {in}, {created}, {cuser}, {updated}, {uuser})
          """ format tableName).on(
        'user_display_name -> user.userGoogleDisplayName,
            'id -> user.userGoogleId, 'name -> user.userGoogleDisplayName,
            'url -> user.userGooglePublicProfileUrl,
            'photo -> user.userGooglePublicPhotoUrl,
            'access -> user.userGoogleAccessToken, 'refresh -> user.userGoogleRefreshToken,
            'at -> user.userGoogleExpiresAt, 'in -> user.userGoogleExpiresIn,
            'created -> currentDate, 'updated -> currentDate,
            'cuser -> user.userGoogleDisplayName,
            'uuser -> user.userGoogleDisplayName).executeUpdate()
      Right(SQL("select last_insert_id() as lastnum from %s" format tableName).apply.head[BigInteger]("lastnum"))
    }
  }

  // IDに一致するUserInfoを一件削除する
  def delete(id: BigInteger): Int = {
    DB.withConnection { implicit conn =>
      SQL("delete from %s where user_id = {id}"
        format tableName).on("id" -> id).executeUpdate
    }
  }

  // user_idが一致する一件だけ取得する
  def selectById(id: BigInteger): Option[UserInfo] = {
    DB.withConnection { implicit conn =>
      SQL("select * from %s  where user_id = {id}"
        format tableName
      ).on('id -> id).as(userInfo *) match {
        case (x :: _) => Some(x)
        case Nil => None
      }
    }
  }

  // user_google_idが一致する一件だけ取得する
  def selectByGoogleId(id: String): Option[UserInfo] = {
    DB.withConnection { implicit conn =>
      SQL("select * from %s where user_google_id = {id}"
        format tableName).on("id" -> id).as(userInfo *) match {
        case (x :: _) => Some(x)
        case Nil => None
      }
    }
  }

  // 渡された内容でUserInfoを更新する
  def update(user:UserInfo) = {
    val currentDate = Calendar.getInstance().getTime();
    DB.withConnection { implicit conn =>
      SQL("""
          update %s set
          user_display_name = {uname},
          user_google_id = {gid}, user_google_display_name = {name},
          user_google_public_profile_url = {url}, user_google_public_profile_photo_url = {photo},
          user_google_access_token = {access}, user_google_refresh_token = {refresh},
          user_google_expires_at = {at}, user_google_expires_in = {in},
          updated_date = {updated}, updated_user = {uuser} where
          user_id = {id}
          """ format tableName).on(
        'uname -> user.userDisplayName,
            'id -> user.userId, 'gid -> user.userGoogleId, 'name -> user.userGoogleDisplayName,
            'url -> user.userGooglePublicProfileUrl,
            'photo -> user.userGooglePublicPhotoUrl,
            'access -> user.userGoogleAccessToken, 'refresh -> user.userGoogleRefreshToken,
            'at -> user.userGoogleExpiresAt, 'in -> user.userGoogleExpiresIn,
            'updated -> currentDate,
            'uuser -> user.userId.toString).executeUpdate()
    }
  }

  def toJson(user: UserInfo): JsValue = {
    implicit val bigIntWriter = Writes[BigInteger] { bi => Json.toJson(bi.longValue()) }
    implicit val dateWriter = Writes[Date] { date => Json.toJson("%tF %<tT" format date)}
    implicit val writer = (
      (__ \ "user_id").write[BigInteger] and
        (__ \ "user_display_name").write[String] and
        (__ \ "google_user_id").write[String] and
        (__ \ "google_display_name").write[String] and
        (__ \ "google_public_profile_url").write[String] and
        (__ \ "google_public_profile_photo_url").write[String] and
        (__ \ "google_access_token").write[String] and
        (__ \ "google_refresh_token").write[String] and
        (__ \ "google_expires_at").write[Long] and
        (__ \ "google_expires_in").write[Long] and
        (__ \ "created_date").write[Date] and
        (__ \ "created_user").write[String] and
        (__ \ "updated_date").write[Date] and
        (__ \ "updated_user").write[String])(unlift(UserInfo.unapply))
    Json.toJson(user)
  }
}
