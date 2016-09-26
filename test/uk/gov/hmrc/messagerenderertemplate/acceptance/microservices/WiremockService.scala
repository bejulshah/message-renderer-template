/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
