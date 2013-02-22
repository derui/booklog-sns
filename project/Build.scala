import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "booklog-sns"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "mysql" % "mysql-connector-java" % "5.1.23",
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
