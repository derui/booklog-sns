package object application {

  import play.api.Play._
  import controllers._

  val Application = new controllers.Application with Secured
  val Rental = new controllers.Rental with Secured
}
