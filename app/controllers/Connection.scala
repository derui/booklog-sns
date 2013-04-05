package controllers

import _root_.util.Composeable
import play.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import java.io.IOException
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Tokeninfo
import models.{UserInforms, UserInform}
import com.google.api.services.plus.Plus
import play.api.mvc.Session
import com.google.api.client.http.GenericUrl
import models.DBWrap.UsePerDB
import scala.language.postfixOps
import java.util.Calendar
import com.google.api.services.plus.model.Person
import java.sql.Timestamp
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

object ConnectResult {

  case class TokenError(e: Exception) extends ConnectResult

  case class MissingAccessToken(message: String) extends ConnectResult

  case class VerificationError(message: String) extends ConnectResult

  case class GoogleApiError(message: String) extends ConnectResult

  case class UserUpdateError(id: Long) extends ConnectResult

  case class UserInsertError(message: String) extends ConnectResult

  case class UserNotFoundError(id: Long) extends ConnectResult

  case class UserNotAuthorizedError(m: String) extends ConnectResult

}

sealed abstract class ConnectResult {
}

/**
 * Google+ Sign-in の機能を利用した認証機能を提供するオブジェクト
 */
object Connection extends UsePerDB with Composeable {

  // 認証情報をセッションに登録する際に利用されるキー
  private val SESSION_KEY = "me"

  // このクラスの結果を表す型
  type Result[A] = Either[ConnectResult, A]

  private val CLIENT_ID: String = current.configuration.getString("application.client_id").get
  private val SECRET_ID: String = current.configuration.getString("application.secret_id").get

  private val JSON_FACTORY: JsonFactory = new JacksonFactory()
  private val TRANSPORT: HttpTransport = new NetHttpTransport()

  // ユーザーが認証されているかどうかをチェックする。
  def checkAuthorization(session: Session): Result[Long] = {
    session.get(SESSION_KEY) match {
      case Some(userId: String) => Right(userId.toLong)
      case None => Left(ConnectResult.UserNotAuthorizedError("User not authorized"))
    }
  }

  /**
   * 画面側から渡されたTokenDataを使って、Google+ との認証を行い、
   * 認証が完了したユーザー情報を返す。
   */
  def connect(token: Token.TokenData): Result[UserInform] = {

    // Credential オブジェクトを作成する
    val credential: GoogleCredential =
      new GoogleCredential.Builder()
        .setJsonFactory(JSON_FACTORY)
        .setTransport(TRANSPORT)
        .setClientSecrets(CLIENT_ID, SECRET_ID).build()
    // 作成したcredentialを使って、OAuth2の認証を行う
    settingCredential(credential, token).right.flatMap(credent =>
      verifyToken(credent)).right.flatMap {
      case (`credential`, id) => saveTokenForUser(id, credential)
    }
  }

  // セッションから、ユーザーのセッション情報のみを削除する。
  def disconnectWithoutUserInfo(session: Session): Result[Session] = {
    checkAuthorization(session).right.flatMap {
      userId =>
        Right(session - SESSION_KEY)
    }
  }

  // 現在セッションに保存されている認証情報と、
  // ユーザーに関連づく各種情報を削除する
  def disconnect(session: Session): Result[Session] = {
    checkAuthorization(session).right.flatMap {
      userId =>
        db withTransaction {
          implicit ds =>
            val q = for {u <- UserInforms if u.userId === userId} yield (u)
            q.firstOption match {
              case None => Left(ConnectResult.UserNotAuthorizedError("User not registered"))
              case Some(user) =>
                // TODO 実際にDisconnectされた際の処理
                // revokeを行い、次回にconnectされた際に情報を保存しておかれるようにする
                TRANSPORT.createRequestFactory().buildGetRequest(
                  new GenericUrl(
                    "https://accounts.google.com/o/oauth2/revoke?token=%s"
                      format
                      user.userGoogleAccessToken)).execute()
                q.delete
                Right(session - SESSION_KEY)
            }
        }
    }
  }

  // Google+で認証されたユーザーを新規に登録する
  private def registerUser(userId: String, credential: GoogleCredential): Result[UserInform] = {
    val plus: Plus = new Plus.Builder(TRANSPORT, JSON_FACTORY, credential).build()
    try {
      Option(plus.people().get("me")).map((get) => get.execute) match {
        case None => Left(ConnectResult.GoogleApiError("Cannot get person data"))
        case Some(profile: Person) =>
          val nowDate = new Timestamp(Calendar.getInstance().getTimeInMillis)
          db.withSession {
            implicit session =>
              val id = UserInforms.ins.insert(
                profile.getDisplayName,
                profile.getId,
                profile.getDisplayName,
                profile.getUrl,
                profile.getImage.getUrl,
                credential.getAccessToken,
                Option(credential.getRefreshToken),
                Option(credential.getExpirationTimeMilliseconds),
                Option(credential.getExpiresInSeconds),
                nowDate,
                0L,
                nowDate,
                0L
              )
              Logger.info(UserInforms.ins.insertStatement)

              val query = for {u <- UserInforms if u.userId === id} yield (u)
              query.map(r => r.createdUser ~ r.updatedUser).update((id, id))
              Logger.info(query.updateStatement)
              Right(query.first)
          }
      }
    } catch {
      case e: IOException => Left(ConnectResult.GoogleApiError(e.getMessage))
    }
  }


  /**
   * 取得したユーザーIDとトークンをDBに保存し、ユーザーに対するトークンを返す。
   */
  def saveTokenForUser(userId: String, credential: GoogleCredential): Result[UserInform] = {
    db.withTransaction {
      implicit session =>
        val q = for {u <- UserInforms if u.userGoogleId === userId} yield (u)
        q.firstOption match {
          case None => registerUser(userId, credential)
          case Some(user) =>
            val expire: Option[Long] = Option(credential.getExpirationTimeMilliseconds)
            val expireIn: Option[Long] = Option(credential.getExpiresInSeconds)
            q.map(r => r.userGoogleRefreshToken ~ r.userGoogleExpiresAt ~ r.userGoogleExpiresIn).update(
              (Option(credential.getRefreshToken).orElse(user.userGoogleRefreshToken),
                expire.orElse(user.userGoogleExpiresAt),
                expireIn.orElse(user.userGoogleExpiresIn)
                )) match {
              case 0 => Left(ConnectResult.UserUpdateError(user.userId))
              case _ => {
                Logger.info(q.updateStatement)
                Right(q.first)
              }
            }
        }
    }
  }

  /**
   * トークンをVerifyし、Googleから提供されたuserTokenを取得する
   *
   */
  private def verifyToken(credential: GoogleCredential): Result[(GoogleCredential, String)] = {
    // tokenが正常かどうかをチェックする
    val oauth2 = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).build

    // トークン情報を取得する
    val tokenInfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken).execute()

    tokenInfo.containsKey("error") match {
      case true => Left(ConnectResult.VerificationError(tokenInfo.get("error").toString))
      case false =>
        expiresSetting(credential, tokenInfo)
        val reg = "^([0-9]*)(.*).apps.googleusercontent.com$".r
        val issuedTo = reg.findFirstMatchIn(CLIENT_ID)
        val localId = reg.findFirstMatchIn(tokenInfo.getIssuedTo)

        (issuedTo, localId) match {
          case (None, _) | (_, None) => Left(ConnectResult.VerificationError("Token's client ID does not match"))
          case (Some(matchIssued), Some(matchLocalId))
            if !matchIssued.group(1).equals(matchLocalId.group(1)) =>
            Left(ConnectResult.VerificationError("Token's client ID does not match"))
          case (_, _) => Right((credential, tokenInfo.getUserId))
        }
    }
  }

  /**
   * expiresが設定されていない場合に設定したCredentialを返す
   */
  private def expiresSetting(credential: GoogleCredential, tokenInfo: Tokeninfo): GoogleCredential = {
    Option(credential.getExpiresInSeconds) match {
      case None =>
        val expireIn: Int = tokenInfo.getExpiresIn
        credential.setExpiresInSeconds(expireIn)
        credential.setExpirationTimeMilliseconds(System.currentTimeMillis() + expireIn * 1000)
        credential
      case Some(_) => credential
    }
  }

  /**
   * Credentialの各種設定を行う
   */
  private def settingCredential(credential: GoogleCredential, token: Token.TokenData): Result[GoogleCredential] = {
    (token.code, token.access_token) match {
      case (None, None) => Left(ConnectResult.MissingAccessToken("Missing access token in request"))
      case (Some(code), _) =>
        exchangeCode(code).right map {
          exchange =>
            credential.setFromTokenResponse(exchange)
        }
      case (None, Some(access_token)) =>
        credential.setAccessToken(access_token)
          .setRefreshToken(token.refresh_token.getOrElse(""))
          .setExpirationTimeMilliseconds(token.expires_at)
        token.expires_in.map {
          credential.setExpiresInSeconds(_)
        }

        Right(credential)
    }
  }

  /**
   * フロントエンドとトークンの交換を行う。
   */
  private def exchangeCode(code: String): Result[GoogleTokenResponse] = {
    try {
      val tokenResponse: GoogleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
        TRANSPORT, JSON_FACTORY, CLIENT_ID, SECRET_ID, code,
        "postmessage").execute()
      Right(tokenResponse)
    } catch {
      case e: Exception => Left(ConnectResult.TokenError(e))
    }
  }

  // ユーザーをセッションに登録する。これは認証の最後のプロセスとなる
  def addAuthToSession(session: Session, userInfo: UserInform): Session = {
    session + (SESSION_KEY -> userInfo.userId.toString)
  }

}

object Token {

  // Google+ との認証で利用するトークンデータ
  case class TokenData(
                        access_token: Option[String], // アクセストークン
                        refresh_token: Option[String], // 新しいトークンが必要なときに利用するrefresh token
                        code: Option[String], // access/refreshトークンのペアを交換するときに利用する、認証コード
                        id_token: String, // ユーザートークン
                        expires_at: Long, // トークンの有効期限
                        expires_in: Option[Long] // トークンの有効時間
                        )

  /**
   * TokenDataをJsonに変換する
   */
  def toJson(token: TokenData): JsValue = {
    implicit val writer: Writes[TokenData] = (
      (__ \ "access_token").write[Option[String]] ~
        (__ \ "refresh_token").write[Option[String]] ~
        (__ \ "code").write[Option[String]] ~
        (__ \ "id_token").write[String] ~
        (__ \ "expires_at").write[Long] ~
        (__ \ "expires_in").write[Option[Long]])(unlift(TokenData.unapply))
    Json.toJson(token)
  }

  /**
   * 渡されたJson文字列から、TokenDataを取得する。取得できなければLeft
   */
  def fromJson(json: JsValue): Either[String, TokenData] = {
    implicit val reader: Reads[TokenData] = (
      (__ \ "access_token").readNullable[String] ~
        (__ \ "refresh_token").readNullable[String] ~
        (__ \ "code").readNullable[String] ~
        (__ \ "id_token").read[String] ~
        (__ \ "expires_at").read[Long] ~
        (__ \ "expires_in").readNullable[Long])(TokenData)

    val expireTransformer = (__ \ "expires_at").json.update(
      __.read[JsString].map {
        case JsString(str) => JsNumber(
          BigDecimal.int2bigDecimal(Integer.valueOf(str)))
      }) andThen
      (__ \ "expires_in").json.update(
        __.read[JsString].map {
          case JsString(str) => JsNumber(
            BigDecimal.int2bigDecimal(Integer.valueOf(str)))
        })
    json.transform(expireTransformer) match {
      case JsError(e) => Left(e.toString())
      case JsSuccess(v, _) =>
        Json.fromJson(v) match {
          case JsError(e) => Left(e.toString())
          case JsSuccess(data, _) => Right(data)
        }
    }
  }
}
