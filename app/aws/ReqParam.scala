package aws

import java.net.URLEncoder

case class Param(key : String, value : String)

object ReqParam {
  // キーをソートする。
  def sortByKey(list:List[Param]): List[Param] =
    list.sortWith((x, y) => x.key.compareToIgnoreCase(y.key) < 0)

  // 渡されたParamをURLエンコードして=で繋いだ形で返す。
  def encode(param:Param): String = {
    val encoder = URLEncoder.encode(_:String, "UTF-8")
      .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    encoder(param.key) + "=" + encoder(param.value)
  }

  // Paramのリストを、&で繋がれたパラメータ文字列に変換する。
  def toString(params: List[Param]): String = {
    def intersperse : (List[String], String) => List[String] = (x:List[String], sep:String) => 
    x match {
      case Nil => List()
      case h :: Nil => List(h)
      case h :: snd :: rest => h :: sep :: snd :: intersperse(rest, sep)
    }
    intersperse(params.map(x => encode(x)), "&").foldLeft(new StringBuilder()){
      (base, x) => base.append(x)
    }.toString
  }
}



