package controllers

import models.Book
import models.BookShelf
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc._
import java.math.BigInteger
import play.api.mvc.Results
import play.api.mvc.SimpleResult
import util._
import models.BookRegister
import models.BookShelf
import models.Book
import models.UserInfo
import controllers.Connection._

trait Application extends Controller with JsonResponse with Composable {
  this : Security =>

  def index = Action {
    Ok(views.html.app())
  }

  // JavaScript側から、oauth2で認証されたトークンを受け取って、二段階目の認証を行う。
  // 認証結果として正常であれば、以下のフォーマットと一致するjsonをレスポンスとして返す。
  // すでに認証されている場合には、認証されているユーザーを取得して返す。
  /**
   * {
   *   "id":0,
   *   "googleUserId":"",
   *   "googleDisplayName":"",
   *   "googlePublicProfileUrl":"",
   *   "googlePublicProfilePhotoUrl":"",
   *   "googleExpiresAt":0
   * }
   */
  def connect = Action { implicit request =>
    request.body.asJson match {
      case None => BadRequest("Auth request must be json format!")
      case Some(json) =>
        Token.fromJson(json) match {
          case Left(e) => BadRequest(e)
          case Right(token) => {
            Connection.connect(token) match {
              case Left(result) => resultToResponse(result)
              case Right(userInfo) =>
                // すでに登録されているユーザーか、今回登録されたユーザー情報を返す。
                // 認証が完了したユーザーについては、セッションに認証データを登録しておく

                Ok((userInfoToToken _ >> responseToJson _)(userInfo)).withSession {
                  Connection.addAuthToSession(session, userInfo)
                }
            }
          }
        }
    }
  }

  // ユーザーをアプリケーションから切り離す。
  def disconnect = Action { implicit request =>
    Connection.disconnect(session) match {
      case Left(e) => resultToResponse(e)
      case Right(session) =>
        Ok("disconnect complete").withSession{ session}
    }
  }

  // ConnectResultをレスポンス用の文字列に変換する
  private def resultToResponse[String](result : ConnectResult) : PlainResult = {
    result match {
      case ConnectResult.TokenError(e) => BadRequest(e.getMessage)
      case ConnectResult.MissingAccessToken(m) => BadRequest(m)
      case ConnectResult.VerificationError(m) => BadRequest(m)
      case ConnectResult.GoogleApiError(m) => BadRequest(m)
      case ConnectResult.UserUpdateError(id) => BadRequest("To update faied at %d" format id)
      case ConnectResult.UserInsertError(m) => BadRequest(m)
      case ConnectResult.UserNotFoundError(id) => BadRequest("User not found : id = %d" format id)
      case ConnectResult.UserNotAuthorizedError(e) => Unauthorized(e)
    }
  }

  // 渡されたUserInfoを返却用のjsonに変換する。
  private def userInfoToToken(userinfo:UserInfo) : List[JsValue] = {
    val json = UserInfo.toJson(userinfo)
    List(Json.obj("id" -> (json \ "user_id").as[Long],
             "googleUserId" -> (json \ "google_user_id").as[String],
             "googleDisplayName" -> (json \ "google_display_name").as[String],
             "googlePublicProfileUrl" -> (json \ "google_public_profile_url").as[String],
             "googlePublicProfilePhotoUrl" -> (json \ "google_public_profile_photo_url").as[String],
             "googleExpiresAt" -> (json \ "google_expires_at").as[Long]))
  }

  case class Shelf(name: String)

  def makeShelf = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "shelf_name" -> nonEmptyText,
          "shelf_description" -> nonEmptyText))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          val result = BookShelf.insert(p._1, p._2)
          OkJsonOneOf(Json.obj("id" -> result.longValue))
        })
    }
  }

  // 登録済みのshelfに対して、データを登録する
  // 指定されたshelfが存在しない場合は登録は行われない。
  def makeBookInShelf = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "shelf_id" -> number,
          "book_name" -> nonEmptyText,
          "book_author" -> text,
          "book_isbn" -> text))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          Book.insert(BookRegister(BigInteger.valueOf(p._1), p._2, p._3, p._4)) match {
            case Left(_) => BadRequest(Json.obj("error" -> "指定された本棚が存在しません"))
            case Right(result) => OkJsonOneOf(Json.obj("id" -> result.longValue))
          }
        })
    }
  }


  // 指定された場合は指定された件数のみ、指定されない場合は全件取得する
  def getAllShelf = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "start" -> optional(number),
          "rows" -> optional(number)))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          val jsoned = BookShelf.all(p._1, p._2).map(BookShelf.toJson)
          OkJson(jsoned)
        })
    }
  }

  // 指定された本棚を削除する
  def deleteBookShelf(id: Long) = Authenticated {
    Action {
      val deleted = BookShelf.delete(BigInteger.valueOf(id))
      if (deleted == 1) {
        OkJson(List())
      } else {
        BadRequest(Json.obj("error" -> "指定された本棚が存在しません"))
      }
    }
  }

  // 指定されたshelfに紐づくbookを取得する
  def getBooksInShelf = Authenticated {
    Action { implicit request =>
      val form = Form(
        tuple(
          "shelf" -> number,
          "start" -> optional(number),
          "rows" -> optional(number)))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          val books = Book.allInShelf(BigInteger.valueOf(p._1), p._2, p._3).map(Book.toJson)
          OkJson(books)
        })
    }
  }

  // 1件だけ取得する
  def getShelfDetail(id: Long) = Authenticated {
    Action {
      (BookShelf.selectById _ << BigInteger.valueOf)(id) match {
        case None => OkJson(List())
        case Some(x) => OkJsonOneOf(BookShelf.toJson(x))
      }
    }
  }

  // Bookの詳細情報を取得する
  def getBookDetail(id: Long) = Authenticated {
    Action {
      (Book.selectById _ << BigInteger.valueOf)(id) match {
        case None => OkJson(List())
        case Some(x) => OkJsonOneOf(Book.toJson(x))
      }
    }
  }
}
