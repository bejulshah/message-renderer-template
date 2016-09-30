import play.PlayImport.PlayKeys._
import sbt._

object MicroServiceBuild extends Build with MicroService {
  val appName = "message-renderer-template"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  override lazy val playSettings = Seq(
    routesImport ++= Seq("uk.gov.hmrc.messagerenderertemplate.domain._",
                         "uk.gov.hmrc.messagerenderertemplate.controllers.binders._")
  )
}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.4.0",
    "uk.gov.hmrc" %% "play-reactivemongo" % "4.8.0",
    "uk.gov.hmrc" %% "play-authorisation" % "3.3.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "1.1.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "domain" % "3.7.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % "test,it",
    "uk.gov.hmrc" %% "reactivemongo-test" % "1.6.0" % "test",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "it,test",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "it,test",
    "com.github.tomakehurst" % "wiremock" % "1.57" % "test",
    "uk.gov.hmrc" % "http-verbs-test_2.11" % "0.1.0" % "it,test"
  )

  def apply() = compile ++ test
}

