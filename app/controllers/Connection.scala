package controllers

import java.math.BigInteger
import play.api.Play.current
import play.api.db.DB
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
import models.UserInfo
import models.UserInfoRegister
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import play.api.mvc.Session
import com.google.api.client.http.GenericUrl;

object ConnectResult {

  case class TokenError(e: Exception) extends ConnectResult
  case class MissingAccessToken(message: String) extends ConnectResult
  case class VerificationError(message: String) extends ConnectResult
  case class GoogleApiError(message: String) extends ConnectResult
  case class UserUpdateError(id : BigInteger) extends ConnectResult
  case class UserInsertError(message : String) extends ConnectResult
  case class UserNotFoundError(id : BigInteger) extends ConnectResult
  case class UserNotAuthorizedError(m: String) extends ConnectResult
}

sealed abstract class ConnectResult {
}

/**
 * Google+ Sign-in の機能を利用した認証機能を提供するオブジェクト
 */
object Connection {

  // 認証情報をセッションに登録する際に利用されるキー
  private val SESSION_KEY = "me"

  // このクラスの結果を表す型
  type Result[A] = Either[ConnectResult, A]

  private val CLIENT_ID: String = "881447546058.apps.googleusercontent.com";
  private val SECRET_ID: String = "Pa2UhdEQxC_o0HCu8ad-PzzS";

  private val JSON_FACTORY: JsonFactory = new JacksonFactory();
  private val TRANSPORT: HttpTransport = new NetHttpTransport();

  // ユーザーが認証されているかどうかをチェックする。
  def checkAuthorization(session: Session) : Result[BigInteger] = {
    session.get(SESSION_KEY) match {
      case Some(userid:String) => Right(BigInteger.valueOf(Integer.valueOf(userid).longValue))
      case None => Left(ConnectResult.UserNotAuthorizedError("User not authorized"))
    }
  }

  /**
   * 画面側から渡されたTokenDataを使って、Google+ との認証を行い、
   * 認証が完了したユーザー情報を返す。
   */
  def connect(token: Token.TokenData): Result[UserInfo] = {

    // Credential オブジェクトを作成する
    val credential: GoogleCredential =
      new GoogleCredential.Builder()
    .setJsonFactory(JSON_FACTORY)
    .setTransport(TRANSPORT)
    .setClientSecrets(CLIENT_ID, SECRET_ID).build();
    // 作成したcredentialを使って、OAuth2の認証を行う
    settingCredential(credential, token).right.flatMap(credent =>
      verifyToken(credent)).right.flatMap {
        case (credential, id) => saveTokenForUser(id, credential)
      }
  }

  // 現在セッションに保存されている認証情報と、
  // ユーザーに関連づく各種情報を削除する
  def disconnect(session : Session) : Result[Session] = {
    checkAuthorization(session).right.flatMap { userid =>
      UserInfo.selectById(userid) match {
        case None => Left(ConnectResult.UserNotAuthorizedError("User not registered"))
        case Some(user) =>
        // TODO 実際にDisconnectされた際の処理
        // revokeを行い、次回にconnectされた際に情報を保存しておかれるようにする
        TRANSPORT.createRequestFactory().buildGetRequest(
          new GenericUrl(
              "https://accounts.google.com/o/oauth2/revoke?token=%s"
            format
              user.userGoogleAccessToken)).execute()

        UserInfo.delete(userid)
        Right(session - SESSION_KEY)
      }
    }
  }

  // Google+で認証されたユーザーを新規に登録する
  private def registUser(userid:String, credential : GoogleCredential) : Result[UserInfo] = {
    val plus : Plus = new Plus.Builder(TRANSPORT, JSON_FACTORY, credential).build()
    try {
      Option(plus.people().get("me")).map((get) => get.execute) match {
        case None => Left(ConnectResult.GoogleApiError("Cannot get person data"))
        case Some(profile) => {
          val regist = UserInfoRegister(
            profile.getId,
            profile.getDisplayName,
            profile.getUrl,
            profile.getImage().getUrl,
            credential.getAccessToken,
            Option(credential.getRefreshToken).getOrElse(""),
            credential.getExpirationTimeMilliseconds,
            credential.getExpiresInSeconds
          )
          UserInfo.insert(regist) match {
            case Left(e) => Left(ConnectResult.UserInsertError(e))
            case Right(u) => UserInfo.selectById(u).toRight(ConnectResult.UserNotFoundError(u))
          }
        }
      }
    } catch {
      case e: IOException => Left(ConnectResult.GoogleApiError(e.getMessage))
    }
  }


  /**
   * 取得したユーザーIDとトークンをDBに保存し、ユーザーに対するトークンを返す。
   */
  def saveTokenForUser(userid:String, credential : GoogleCredential) : Result[UserInfo] = {
    UserInfo.selectByGoogleId(userid) match {
      case None => registUser(userid, credential)
      case Some(user) =>
        user match {
          case UserInfo(id, gid, name, url, photo, _, refresh, eAt, eIn, created, cuser, updated, uuser) =>
            val update = UserInfo(
              id, gid, name, url, photo, credential.getAccessToken,
              Option(credential.getRefreshToken).getOrElse(refresh),
              credential.getExpirationTimeMilliseconds,
              credential.getExpiresInSeconds,
              created, cuser, updated, uuser
            );
          UserInfo.update(update) match {
            case 0 => Left(ConnectResult.UserUpdateError(user.userId))
            case _ => UserInfo.selectById(user.userId) match {
              case None => Left(ConnectResult.UserNotFoundError(user.userId))
              case Some(u) => Right(u)
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
    val tokenInfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken()).execute()

    tokenInfo.containsKey("error") match {
      case true => Left(ConnectResult.VerificationError(tokenInfo.get("error").toString()))
      case false =>
        expiresSetting(credential, tokenInfo)
        val reg = "^([0-9]*)(.*).apps.googleusercontent.com$".r
        val issuedTo = reg.findFirstMatchIn(CLIENT_ID)
        val localId = reg.findFirstMatchIn(tokenInfo.getIssuedTo())

        (issuedTo, localId) match {
          case (None, _) | (_, None) => Left(ConnectResult.VerificationError("Token's client ID does not match"))
          case (Some(matchIssued), Some(matchLocalId))
          if !matchIssued.group(1).equals(matchLocalId.group(1)) =>
            Left(ConnectResult.VerificationError("Token's client ID does not match"))
          case (_, _) => Right((credential ,tokenInfo.getUserId()));
        }
    }
  }

  /**
   * expiresが設定されていない場合に設定したCredentialを返す
   */
  private def expiresSetting(credential: GoogleCredential, tokenInfo: Tokeninfo): GoogleCredential = {
    Option(credential.getExpiresInSeconds()) match {
      case None =>
        val expireIn: Int = tokenInfo.getExpiresIn();
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
        exchangeCode(code).right map { exchange =>
          credential.setFromTokenResponse(exchange)
        }
      case (None, Some(access_token)) =>
        credential.setAccessToken(access_token)
          .setRefreshToken(token.refresh_token.getOrElse(""))
          .setExpirationTimeMilliseconds(token.expires_at)
        token.expires_in.map { credential.setExpiresInSeconds(_) }

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
        "postmessage").execute();
      Right(tokenResponse)
    } catch {
      case e: Exception => Left(ConnectResult.TokenError(e))
    }
  }

  // ユーザーをセッションに登録する。これは認証の最後のプロセスとなる
  def addAuthToSession(session:Session, userInfo:UserInfo) :Session = {
    session + (SESSION_KEY -> userInfo.userId.toString())
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
    implicit val writer = (
      (__ \ "access_token").writeNullable[String] and
      (__ \ "refresh_token").writeNullable[String] and
      (__ \ "code").writeNullable[String] and
      (__ \ "id_token").write[String] and
      (__ \ "expires_at").write[Long] and
      (__ \ "expires_in").writeNullable[Long])(unlift(TokenData.unapply))
    Json.toJson(token)
  }

  /**
   * 渡されたJson文字列から、TokenDataを取得する。取得できなければNone
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
      __.read[JsString].map { case JsString(str) => JsNumber(
        BigDecimal.int2bigDecimal(Integer.valueOf(str)))
      }) andThen
        (__ \ "expires_in").json.update(
      __.read[JsString].map { case JsString(str) => JsNumber(
        BigDecimal.int2bigDecimal(Integer.valueOf(str)))
      })
    json.transform(expireTransformer) match {
      case JsError(e) => Left(e.toString)
      case JsSuccess(v, _) =>
        Json.fromJson(v) match {
          case JsError(e) => Left(e.toString)
          case JsSuccess(v, _) => Right(v)
        }
    }
  }
}
