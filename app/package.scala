
package object application {

  import play.api.Play._
  import controllers._

  val Application = if (isTest) {
    new controllers.Application with NonSecured
  } else {
    new controllers.Application with Secured
  }

  val Rental = if (isTest) {
    new controllers.Rental with NonSecured
  } else {
    new controllers.Rental with Secured
  }
}
