package aws

import java.io.File
import java.lang.StringBuilder
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.libs.Files
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import scala.io.BufferedSource
import scala.io.Source
import scala.xml.Document
import scala.xml.NodeSeq
import scala.xml.parsing.ConstructingParser
import util.Base64Util
import util.Composeable
import aws.tag.{ItemLookupTag => I}
import util.JsonUtil

trait Request {
  def send(request: AWSRequest): Document
}

case class AWSRequest(params :List[Param], secretKey :String,
  accessKey : String, associateTag : String, amazonURL : String)

object ItemSearch extends Request with Composeable {

  private val signatureAddress = "ecs.amazonaws.jp\n/onca/xml\n"

  // ItemLookupの形式でリクエストを作成する。
  override def send(request : AWSRequest) : Document = {
    val source = Source.fromURL(makeRequestURI(request))
    try {
      ConstructingParser.fromSource(source, false).document
    } finally {
      source.asInstanceOf[BufferedSource].close
    }
  }

  // XMLを返却用に変換する
  def documentToJson(doc:Document) : (JsObject, Int) = {
    val items = itemsToJson(I.items(doc))

    (Json.obj("total_result" -> I.totalResults(doc).text.toInt,
      "total_page" -> I.totalPage(doc).text.toInt,
      "items" -> JsonUtil.listToArray(items)),
      items.length)
  }

  // ItemタグそれぞれについてJsonに変換して返す。
  private def itemsToJson(items:NodeSeq): List[JsObject] = {
    (items.map { item =>
      val attr = I.itemAttributes(item)
      Json.obj("author" -> I.author(attr).text,
        "title" -> I.title(attr).text,
        "isbn" -> I.title(attr).text,
        "ean" -> I.title(attr).text,
        "asin" -> I.asin(attr).text,
        "publication_date" -> I.publicationDate(attr).text,
        "small_image" -> (I.url _ << I.smallImage _)(item).text,
        "medium_image" -> (I.url _ << I.mediumImage _)(item).text,
        "large_image" -> (I.url _ << I.largeImage _)(item).text)
    }).toList
  }

  // 実行時のタイムスタンプを取得する
  private def timestamp:String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    fmt.setTimeZone(TimeZone.getTimeZone("GMT"))
    fmt.format(Calendar.getInstance.getTime)
  }

  // リクエストのパラメータを作成する。
  private def makeRequestURI(req : AWSRequest): String = {
    val timestamp = Param("Timestamp", this.timestamp)

    val sorted = ReqParam.sortByKey(timestamp :: req.params)

    val signature = makeSignature(req.secretKey, "GET\n%s%s" format (signatureAddress,
      ReqParam.toString(sorted)))

    val paramContainSig = sorted ::: List(signature)

    req.amazonURL + "?" + ReqParam.toString(paramContainSig)
  }

  // パラメータと秘密鍵から、signatureを作成する。
  private def makeSignature(secretKey:String, base:String): Param = {
    val spec = new SecretKeySpec(secretKey.getBytes, "HmacSHA256")

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(spec)

    val baseByteArray = base.getBytes("UTF-8")

    val rawHmac = mac.doFinal(baseByteArray)
    Param("Signature", Base64Util.encode(rawHmac))
  }
}


