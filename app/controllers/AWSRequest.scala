package controllers

import aws.{AWSRequest => Request}
import aws.ParamGen
import aws.ParamKey
import aws.ItemSearch
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
import play.api.Play
import play.api.Play.current

trait AWSRequest extends Controller with JsonResponse with Composeable {
  this: Security =>

  val accessKey = current.configuration.getString("aws.aws_access_key").get
  val secretKey = current.configuration.getString("aws.aws_secret_key").get
  val amazonURL = current.configuration.getString("aws.aws_endpoint_url").get
  val associateTag = current.configuration.getString("aws.aws_associate_tag").get

  val commonParam = ParamGen.common(List(), accessKey, associateTag)

  private def makeRequest(param:List[ParamKey]) : Request = {
    Request(ParamGen.format(param), accessKey, secretKey, associateTag, amazonURL)
  }

  def searchBy(keyword:String) = Authenticated {
    Action { implicit request =>
      val form = Form(
        "page" -> optional(number))

      form.bindFromRequest.fold(
        e => BadRequest(e.errors.head.message),
        p => {
          val ope = ParamGen.operation(_:List[ParamKey], "ItemSearch")
          val version = ParamGen.version(_:List[ParamKey])
          val res = ParamGen.resGroup(_:List[ParamKey])
          val keywords = ParamGen.keywords(_:List[ParamKey], keyword)
          val page = ParamGen.itemPage(_:List[ParamKey], p.getOrElse(0))
          val param = (ope << version << res << keywords << page)(commonParam)
          val req = makeRequest(param)

          val document = ItemSearch.send(req)
          ItemSearch.documentToJson(document) match {
            case (items, count) => okJsonRaw(items, count)
          }
        })
    }
  }
}
