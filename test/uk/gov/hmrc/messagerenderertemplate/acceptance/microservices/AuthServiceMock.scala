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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.messagerenderertemplate.domain.Recipient

class AuthServiceMock extends WiremockService("auth", servicePort = 8500) {
  def token = "authToken9349872"

  def succeedsFor(recipient: Recipient) = {
    service.register(get(urlMatching("/auth/authority*"))
      .willReturn(aResponse().withStatus(200).withBody(jsonFor(recipient))))
  }

  def jsonFor(recipient: Recipient) = {
    Json.obj(
      "confidenceLevel" -> 500,
      "uri" -> "testUri",
      "accounts" -> Json.obj(
        recipient.regime -> Json.obj(
          recipient.taxId.name -> recipient.taxId.value
        )
      )
    ).toString()
  }
}
