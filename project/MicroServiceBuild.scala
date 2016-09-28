import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object MicroServiceBuild extends Build with MicroService {
  val appName = "message-renderer-template"
  override
  lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.4.0",
    "uk.gov.hmrc" %% "play-reactivemongo"      % "4.8.0",
    "uk.gov.hmrc" %% "play-authorisation" % "3.3.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "1.1.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "domain" % "3.7.0"
  )

  val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % "test,it",
        "uk.gov.hmrc"       %% "reactivemongo-test"          % "1.6.0" % "test",
        "org.scalatest" %% "scalatest" % "2.2.6" % "test,it",
        "org.pegdown" % "pegdown" % "1.6.0" % "it,test",
        "com.typesafe.play" %% "play-test" % PlayVersion.current % "it,test",
        "com.github.tomakehurst" % "wiremock" % "1.57" % "test"
      )

  def apply() = compile ++ test
}

