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
import play.api.http.Status
import uk.gov.hmrc.messagerenderertemplate.domain.{MessageBodyId, MessageHeader}

class MessageServiceMock(authToken: String, servicePort: Int = 8910)
  extends WiremockService("message", servicePort) {

  def receivedMessageCreateRequestFor(messageHeader: MessageHeader, messageBodyId: MessageBodyId): Unit = {
    service.verifyThat(postRequestedFor(urlEqualTo("/messages")).
      withRequestBody(equalToJson(jsonFor(messageHeader, messageBodyId)))
    )
  }

  def successfullyCreates(messageHeader: MessageHeader): Unit = {
    service.register(post(urlEqualTo("/messages")).
      willReturn(aResponse().withStatus(Status.OK)))
  }

  def returnsDuplicateExistsFor(messageHeader: MessageHeader): Unit = {
    service.register(post(urlEqualTo("/messages")).
      willReturn(aResponse().withStatus(Status.CONFLICT)))
  }

  private def jsonFor(messageHeader: MessageHeader, messageBodyId: MessageBodyId): String = {
    s"""
       | {
       |   ${messageHeader.statutory.fold("")(value => s""""statutory": $value,""")}
       |   "recipient": {
       |     "regime": "${messageHeader.recipient.regime}",
       |     "identifier": {
       |       "${messageHeader.recipient.taxId.name}": "${messageHeader.recipient.taxId.value}"
       |     }
       |   },
       |   "subject": "${messageHeader.subject}",
       |   "hash": "${messageHeader.hash}",
       |   "renderUrl": {
       |     "service": "message-renderer-template",
       |     "url": "/message-renderer-template/messages/${messageBodyId.value}"
       |   }
       | }
       | """.stripMargin
  }
}
