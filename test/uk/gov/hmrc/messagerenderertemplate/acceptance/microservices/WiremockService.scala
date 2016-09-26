package uk.gov.hmrc.messagerenderertemplate.acceptance.microservices

import play.api.Play
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

abstract class WiremockService(serviceName: String,
                               servicePort: Int,
                               serviceHost: String = "localhost") {

  private lazy val wireMockServer = new WireMockServer(wireMockConfig().port(servicePort))
  protected val service = new WireMock(servicePort)

  def start() = {
    wireMockServer.start()
  }

  def stop() = {
    wireMockServer.stop()
  }

  def reset() = {
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
  }

  def fullUrlFor(path: String) = {
    val port = Play.current.configuration.getString(s"${microserviceConfigPathFor(serviceName)}.port").get
    val host = Play.current.configuration.getString(s"${microserviceConfigPathFor(serviceName)}.host").get
    s"http://$host:$port$path"
  }

  def configuration = Map(
    s"${microserviceConfigPathFor(serviceName)}.port" -> s"$servicePort",
    s"${microserviceConfigPathFor(serviceName)}.host" -> serviceHost
  )

  private def microserviceConfigPathFor(serviceName: String) = {
    s"microservice.services.$serviceName"
  }
}
