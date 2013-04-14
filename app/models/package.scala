
package object wrapper {

  import play.api.Play._
  import models._
  import models.connector._

  val ConnectionWrapper = if (isTest) {
    new models.InnerWrapper with TestConnector
  } else {
    new models.InnerWrapper with DefaultConnector
  }
}
