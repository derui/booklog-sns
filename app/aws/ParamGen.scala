package aws

object ParamKey {
  case class Version(v:String) extends ParamKey
  case class Operation(v:String) extends ParamKey
  case class ResponseGroup(v:String) extends ParamKey
  case class IdType(v:String) extends ParamKey
  case class ItemId(v:String) extends ParamKey
  case class SearchIndex(v:String) extends ParamKey
  case class Service(v:String) extends ParamKey
  case class AccessKeyId(v:String) extends ParamKey
  case class AssociateTag(v:String) extends ParamKey
  case class ItemPage(v:String) extends ParamKey
  case class Keywords(v:String) extends ParamKey

  def toParam(k:ParamKey) : Param = {
    k match {
      case Version(v) => Param("Version", v)
      case Operation(v) => Param("Operation", v)
      case ResponseGroup(v) => Param("ResponseGroup", v)
      case IdType(v) => Param("IdType", v)
      case ItemId(v) => Param("ItemId", v)
      case SearchIndex(v) => Param("SearchIndex", v)
      case Service(v) => Param("Service", v)
      case AccessKeyId(v) => Param("AWSAccessKeyId", v)
      case AssociateTag(v) => Param("AssociateTag", v)
      case ItemPage(v) => Param("ItemPage", v)
      case Keywords(v) => Param("Keywords", v)
    }
  }
}

sealed abstract class ParamKey {
}

object ParamGen {

  def format(list:List[ParamKey]) : List[Param] = list.map(ParamKey.toParam(_))

  private def union(x:ParamKey, base:ParamKey) : Boolean =
    ParamKey.toParam(x).key == ParamKey.toParam(base).key

  def version(param:List[ParamKey], version:String = "2011-08-01"): List[ParamKey] = {
    val pk = ParamKey.Version(version)
    pk :: param.filterNot(x => union(x, pk))
  }

  def operation(param:List[ParamKey], default:String = "ItemLookup"): List[ParamKey] = {
    val pk = ParamKey.Operation(default)
    pk :: param.filterNot(x => union(x, pk))
  }

  def resGroup(param:List[ParamKey], group:String = "Medium"): List[ParamKey] = {
    val pk = ParamKey.ResponseGroup(group)
    pk :: param.filterNot(x => union(x, pk))
  }

  def itemPage(param:List[ParamKey], page:Int) : List[ParamKey] = {
    val pk = ParamKey.ItemPage(page.toString)
    if (page > 0) {
      pk :: param.filterNot(x => union(x, pk))
    } else param
  }

  def keywords(param:List[ParamKey], keyword:String) : List[ParamKey] = {
    val pk = ParamKey.Keywords(keyword)
    pk :: param.filterNot(x => union(x, pk))
  }

  def isbn(param:List[ParamKey], id:String): List[ParamKey] = {
    val it = ParamKey.IdType("ISBN")
    val item = ParamKey.ItemId(id)
    it :: item :: param.filterNot(x => union(x, it) || union(x, item))
  }

  def ean(param:List[ParamKey], id:String) : List[ParamKey] = {
    val it = ParamKey.IdType("EAN")
    val item = ParamKey.ItemId(id)
    it :: item :: param.filterNot(x => union(x, it) || union(x, item))
  }

  def asin(param:List[ParamKey], id:String) : List[ParamKey] = {
    val it = ParamKey.IdType("ASIN")
    val item = ParamKey.ItemId(id)
    it :: item :: param.filterNot(x => union(x, it) || union(x, item))
  }

  def common(param:List[ParamKey], accessKey:String, associate:String):List[ParamKey] = {
    val si = ParamKey.SearchIndex("Books")
    val svc = ParamKey.Service("AWSECommerceService")
    val akey = ParamKey.AccessKeyId(accessKey)
    val skey = ParamKey.AssociateTag(associate)

    si :: svc :: akey :: skey ::
    param.filterNot(x => union(x, si) || union(x, svc) || union(x, akey) || union(x, skey))
  }
}
