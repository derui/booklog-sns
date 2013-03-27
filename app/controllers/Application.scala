package controllers

import util._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._
import java.sql.Date
import java.util.Calendar
import scala.slick.driver.MySQLDriver.simple.{Session => _, _}
import scala.slick.driver.MySQLDriver.simple.{Session => DBSession}
import models.DBWrap.UsePerDB

trait Application extends Controller with JsonResponse with Composeable with UsePerDB {
  this: Security =>

  def index = Action {
    Ok(views.html.index())
  }

  def registerBookShelf = Action {
    Ok(views.html.registerBookShelf())
  }

  def showBookShelf(id: Long) = Action {
    Ok(views.html.showBookShelf())
  }

  def registerBook = Action {
    Ok(views.html.registerBook())
  }

  def showBook(id: Long) = Action {
    Ok(views.html.showBook())
  }

  // JavaScript側から、oauth2で認証されたトークンを受け取って、二段階目の認証を行う。
  // 認証結果として正常であれば、以下のフォーマットと一致するjsonをレスポンスとして返す。
  // すでに認証されている場合には、認証されているユーザーを取得して返す。
  /**
   * {
   * "id":0,
   * "googleUserId":"",
   * "googleDisplayName":"",
   * "googlePublicProfileUrl":"",
   * "googlePublicProfilePhotoUrl":"",
   * "googleExpiresAt":0
   * }
   */
  def connect = Action {
    implicit request =>
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

  // ユーザーをログアウトする。Google+の認証だけを切断し、次回のログイン時には
  // 権限の認証は不要とするような状態にする。
  def logout = Authenticated {
    Action {
      implicit request =>
        Connection.disconnectWithoutUserInfo(session) match {
          case Left(e) => resultToResponse(e)
          case Right(session) =>
            Ok("logout complete").withSession {
              session
            }
        }
    }
  }

  // ユーザーをアプリケーションから切り離す。
  def disconnect = Authenticated {
    Action {
      implicit request =>
        Connection.disconnect(session) match {
          case Left(e) => resultToResponse(e)
          case Right(session) =>
            Ok("disconnect complete").withSession {
              session
            }
        }
    }
  }

  // ConnectResultをレスポンス用の文字列に変換する
  private def resultToResponse[String](result: ConnectResult): PlainResult = {
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
  private def userInfoToToken(userInfo: UserInform): List[JsValue] = {
    val json = UserInforms.toJson(userInfo)
    List(Json.obj("id" -> (json \ "user_id").as[Long],
      "googleUserId" -> (json \ "google_user_id").as[String],
      "googleDisplayName" -> (json \ "google_display_name").as[String],
      "googlePublicProfileUrl" -> (json \ "google_public_profile_url").as[String],
      "googlePublicProfilePhotoUrl" -> (json \ "google_public_profile_photo_url").as[String],
      "googleExpiresAt" -> (json \ "google_expires_at").as[Long]))
  }

  case class Shelf(name: String)

  def makeShelf = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          tuple(
            "shelf_name" -> nonEmptyText,
            "shelf_description" -> nonEmptyText))

        form.bindFromRequest.fold(
          e => BadRequest(e.errors.head.message),
          p => {
            db withSession {
              implicit ds =>
                val now = new Date(Calendar.getInstance().getTimeInMillis)
                val result = BookShelves.ins.insert(
                  p._1, p._2, now, getAuthUserId, now, getAuthUserId)
                okJsonOneOf(Json.obj("id" -> result))
            }
          })
    }
  }

  // 登録済みのshelfに対して、データを登録する
  // 指定されたshelfが存在しない場合は登録は行われない。
  def makeBookInShelf = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          tuple(
            "shelf_id" -> number,
            "book_name" -> nonEmptyText,
            "book_author" -> optional(text),
            "book_isbn" -> optional(text),
            "published_date" -> optional(date("yyyy/MM/dd")),
            "large_image_url" -> optional(text),
            "medium_image_url" -> optional(text),
            "small_image_url" -> optional(text)
          ))

        form.bindFromRequest.fold(
          e => BadRequest(e.errors.head.message),
          p => {
            db.withTransaction {
              implicit ds =>
                val now = new Date(Calendar.getInstance().getTimeInMillis)
                val result = Books.ins.insert(p._1, p._2, p._3, p._4,
                  p._5.map(s => new Date(s.getTime)), p._6, p._7, p._8, now, getAuthUserId, now, getAuthUserId)

                okJsonOneOf(Json.obj("id" -> result))
            }
          })
    }
  }

  // 指定された場合は指定された件数のみ、指定されない場合は全件取得する
  def getAllShelf = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          tuple(
            "start" -> optional(number),
            "rows" -> optional(number)))

        form.bindFromRequest.fold(
          e => BadRequest(e.errors.head.message),
          p => {
            db.withSession {
              implicit ds =>
                okJson(BookShelves.findAll(p._1, p._2).map(r => (JsonUtil.jsonWithUserName(r._1, r._2, r._3, BookShelves.toJson _))))
            }
          })
    }
  }

  // 指定された本棚を削除する
  def deleteBookShelf(id: Long) = Authenticated {
    Action {
      db.withTransaction {
        implicit ds: DBSession =>
          val deleted = (for {
            b <- BookShelves
            if b.id === id
          } yield b).delete
          if (deleted == 1) {
            okJson(List())
          } else {
            BadRequest(Json.obj("error" -> "指定された本棚が存在しません"))
          }
      }
    }
  }

  // 指定されたshelfに紐づくbookを取得する
  def getBooksInShelf = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          tuple(
            "shelf" -> number,
            "start" -> optional(number),
            "rows" -> optional(number)))

        form.bindFromRequest.fold(
          e => BadRequest(e.errors.head.message),
          p => {
            db.withSession {
              implicit ds =>
                val books = Books.findAllInShelf(p._1, p._2, p._3).map(r =>
                  JsonUtil.jsonWithUserName(r._1, r._2, r._3, Books.toJson _))
                okJson(books)
            }
          })
    }
  }

  // 本棚の情報を1件だけ取得する
  def getShelfDetail(id: Long) = Authenticated {
    Action {
      db withSession {
        implicit ds: DBSession =>
          BookShelves.selectById(id) match {
            case None => okJson(List())
            case Some(x) => {
              // 本棚に関連づいているbookの一覧を取得する。
              val bookToJson = (x: List[Books.BookWithName]) => x.map {
                case (Book(bid, _, name, author, isbn, publish, l, m, s, _, _, _, _), _, _) =>
                  Json.obj("book_id" -> bid, "book_name" -> name, "book_author" -> author,
                    "book_isbn" -> isbn, "published_date" -> publish,
                    "large_image_url" -> l, "medium_image_url" -> m,
                    "small_image_url" -> s)
              }
              val bookUrls = (bookToJson << (Books.findAllInShelf(_: Long, None, None)))(id)
              // jsonの末尾にbooks -> [..]という形式でプロパティを追加する
              val transformer = (__).json.update(
                __.read[JsObject].map {
                  o => o ++ Json.obj("books" -> JsonUtil.listToArray(bookUrls))
                })
              val tupled = (x: BookShelves.BookShelfWithName) => JsonUtil.jsonWithUserName(x._1, x._2, x._3, BookShelves.toJson _)
              tupled(x).transform(transformer).fold(
                invalid => BadRequest(Json.obj("error" -> invalid.toString)),
                valid => okJsonOneOf(valid)
              )
            }
          }
      }
    }
  }

  // Bookの詳細情報を取得する
  def getBookDetail(id: Long) = Authenticated {
    Action {
      db withSession {
        implicit ds: DBSession =>
          Books.selectById(id) match {
            case None => BadRequest(Json.obj("error" -> "指定された本が存在しません"))
            case Some(x) => okJsonOneOf(JsonUtil.jsonWithUserName(x._1, x._2, x._3, Books.toJson _))
          }
      }
    }
  }

  // ログインユーザーの情報を更新する。
  def updateUserInfo() = Authenticated {
    Action {
      implicit request =>
        val form = Form(
          "user_display_name" -> text
        )

        form.bindFromRequest.fold(
          e => BadRequest(e.errors.head.message),
          p => {
            db withTransaction {
              implicit ds =>
                UserInforms.selectById(getAuthUserId) match {
                  case None => BadRequest(Json.obj("error" -> "ログインされていません"))
                  case Some(res) => {
                    val q = for {u <- UserInforms
                                 if u.userId === res.userId
                    } yield (u.userDisplayName)
                    q.update(p)
                    okJson(List())
                  }
                }
            }
          })
    }
  }

  // ログインユーザーの情報を取得する。
  def getLoginUserInfo = Authenticated {
    Action {
      db.withSession {
        implicit ds: DBSession =>
          UserInforms.selectById(getAuthUserId) match {
            case None => BadRequest(Json.obj("error" -> "ユーザーの情報が見つかりません"))
            case Some(res) => okJsonOneOf(UserInforms.toJson(res))
          }
      }
    }
  }
}
