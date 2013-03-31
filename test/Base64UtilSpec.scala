package test

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.test._
import play.api.test.Helpers._
import util.Base64Util

class Base64UtilSpec extends Specification {

  "Base64Util" should {
    "be able to encode byte array to be encoded string" in {

      val sample1 = "ABCDEFG"
      Base64Util.encode(sample1.getBytes()) must be_==("QUJDREVGRw==")

      val sample2 = "テスト"
      Base64Util.encode(sample2.getBytes()) must be_==("44OG44K544OI")

      val sample3 = """GET
webservices.amazon.com
/onca/xml
AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&ItemId=0679722769&Operation=ItemLookup&ResponseGroup=ItemAttributes%2COffers%2CImages%2CReviews&Service=AWSECommerceService&Timestamp=2009-01-01T12%3A00%3A00Z&Version=2009-01-06"""
      val spec = new SecretKeySpec("1234567890".getBytes(), "HmacSHA256")

      val mac = Mac.getInstance(spec.getAlgorithm())
      mac.init(spec)

      val rawHmac = mac.doFinal(sample3.getBytes("UTF-8"))
      Base64Util.encode(rawHmac) must be_==("M/y0+EAFFGaUAp4bWv/WEuXYah99pVsxvqtAuC8YN7I=")
    }
  }
}
