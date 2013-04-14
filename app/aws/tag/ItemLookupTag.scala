package aws.tag

import scala.xml.Document
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.XML

object ItemLookupTag {

  // ルートに存在するタグ
  def itemSearchResponse(doc:NodeSeq): NodeSeq = doc \ "ItemSearchResponse"
  def itemLookupResponse(doc:NodeSeq): NodeSeq = doc \ "ItemLookupResponse"

  def isValid(doc:NodeSeq) : NodeSeq = doc \\ "IsValid"

  // ItemSearchResponse直下に存在するタグ
  def items(doc:NodeSeq): NodeSeq = doc \\ "Item"
  def totalPage(doc:NodeSeq): NodeSeq = doc \\ "TotalPages"
  def totalResults(doc:NodeSeq): NodeSeq = doc \\ "TotalResults"

  // Item内に存在するタグ
  def itemAttributes(doc:NodeSeq):NodeSeq = doc \ "ItemAttributes"
  def smallImage(doc:NodeSeq) : NodeSeq = doc \ "SmallImage"
  def mediumImage(doc:NodeSeq) : NodeSeq = doc \ "MediumImage"
  def largeImage(doc:NodeSeq) : NodeSeq = doc \ "LargeImage"

  // small/medium/largeImageの配下にあるタグ
  def url(doc:NodeSeq) : NodeSeq = doc \ "URL"
  
  // ItemAttributesタグ配下にあるタグ
  def author(doc:NodeSeq):NodeSeq = doc \ "Author"
  def title(doc:NodeSeq):NodeSeq = doc \ "Title"
  def isbn(doc:NodeSeq):NodeSeq = doc \ "ISBN"
  def ean(doc:NodeSeq):NodeSeq = doc \ "EAN"
  def publicationDate(doc:NodeSeq):NodeSeq = doc \ "PublicationDate"
  def asin(doc:NodeSeq):NodeSeq = doc \ "ASIN"
}
