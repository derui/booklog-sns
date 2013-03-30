package util

object Base64Util {

  private val base64Char : Array[Char] = Array(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
  )

  // 24bit単位での変換を行う。
  private def encode24(baseData : Int, srcLength : Int) : String = {
    val doMask = (x:Int, shift:Int) => x >> shift & 0x3f
    val masks = List(doMask(_:Int, 18), doMask(_:Int, 12), doMask(_:Int, 6),
      doMask(_:Int, 0))

    val base = baseData << (2 - srcLength)
    val chars = masks.drop(srcLength + 2).map{f => f(base)}.map(x => base64Char(x))
    (chars ::: List.fill(4 - chars.length)('=')).foldLeft(new StringBuilder()) { (builder, x) =>
      builder.append(x)
    }.toString
  }

  // 渡されたバイト列をbase64に変換する
  def encode(data :Array[Byte]): String = {
    val dataList = data.toList
    def encodePer24 : (List[Int]) => List[String] = list => list match {
      case Nil => List()
      case x :: Nil => List(encode24(x, 1))
      case x :: y :: rest => encode24(x << 8 | y, 2) :: encodePer24(rest)
    }

    encodePer24(dataList.map(x => if (x < 0) x + 256 else x))
      .foldLeft(new StringBuilder()) { (builder, x) =>
      builder.append(x)
    }.toString
  }
}
