package aws

import scala.xml.Document

case class AWSRequest(params :List[Param], secretKey :String,
  accessKey : String, associateTag : String, amazonURL : String)

trait Request {
  def send(request: AWSRequest): Document
}

