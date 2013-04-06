package controllers.barcode

import scala.collection.JavaConverters._
import com.google.zxing.common.{HybridBinarizer}
import com.google.zxing._
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.imageio._
import play.api.Logger

// 渡されたデータについて、EAN形式のバーコードであるかどうかを返す。
// 渡されるファイルは、原則PNGでなければならない。
object EANBarcode {

  type Barcode = String

  // 渡されたファイルから画像を読み込み、バーコード判別を行う。
  def decode(imageFile:File): Either[String, Barcode] = {

    readImage(imageFile).map {image =>
      val rgbsource = new RGBLuminanceSource(image.getWidth,
        image.getHeight,
        imageToPixelArray(image))
      val bitmap = new BinaryBitmap(new HybridBinarizer(rgbsource))
      val hints = Map(DecodeHintType.TRY_HARDER -> true)
      decodeBarcode(bitmap, hints)
    }.orElse(Some(Left("画像が読み込めません"))).get
  }

  // PNGファイルを読み込む。読み込めなかった場合にはNoneを返す
  private def readImage(image:File) : Option[BufferedImage] = {
    try {
      Option(ImageIO.read(image))
    } catch {
      case e: Exception =>
        Logger.info("image can't read : %s" format e.getStackTrace().toString())
        None
    }
  }

  // imageからRGBのPixel配列を取得する
  private def imageToPixelArray(image:BufferedImage): Array[Int] = {
    val imaged = for {
      y <- Range(0, image.getHeight)
      x <- Range(0, image.getWidth)
    } yield image.getRGB(x, y)
    imaged.toArray
  }

  // 渡された画像をDecodeし、結果がEANであれば、内容を返す。
  private def decodeBarcode(bitmap: BinaryBitmap,
    hint: Map[DecodeHintType, Boolean]) : Either[String, Barcode] = {
    val reader = new MultiFormatReader()

    try {
      val result = reader.decode(bitmap, hint.asJava)
      result.getBarcodeFormat() match {
        case BarcodeFormat.EAN_8 | BarcodeFormat.EAN_13 => Right(result.getText)
        case _ => Left("サポート外のバーコードです")
      }
    } catch {
      case e: Exception =>
        Logger.info("barcode decoding failure")
        e.printStackTrace
        Left("バーコード解析に失敗しました")
    }
  }
}
