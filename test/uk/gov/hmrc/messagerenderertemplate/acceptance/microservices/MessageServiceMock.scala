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

import java.security.MessageDigest

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.commons.codec.binary.Base64
import play.api.http.Status
import uk.gov.hmrc.messagerenderertemplate.domain.{MessageBody, MessageHeader}
import uk.gov.hmrc.play.controllers.RestFormats.localDateFormats

class MessageServiceMock(authToken: String, servicePort: Int = 8910)
  extends WiremockService("message", servicePort) {

  def receivedMessageCreateRequestFor(messageHeader: MessageHeader, messageBody: MessageBody): Unit = {
    service.verifyThat(postRequestedFor(urlEqualTo("/messages")).
      withRequestBody(equalToJson(jsonFor(messageHeader, messageBody)))
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

  private def jsonFor(messageHeader: MessageHeader, messageBody: MessageBody): String = {
    s"""
       | {
       |   ${messageHeader.statutory.fold("")(value => s""""statutory": $value,""")}
       |   "recipient": {
       |     "regime": "${messageHeader.recipient.regime}",
       |     "identifier": {
       |       "${messageHeader.recipient.identifier.name}": "${messageHeader.recipient.identifier.value}"
       |     }
       |   },
       |   "subject": "${messageHeader.subject}",
       |   "hash": "${hash(Seq("message-renderer-template", messageHeader.subject, messageBody.content))}",
       |   "validFrom": "${messageHeader.validFrom}",
       |   "renderUrl": {
       |     "service": "message-renderer-template",
       |     "url": "/message-renderer-template/messages/${messageBody.id.value}"
       |   },
       |   "alertDetails": {
       |       "templateId": "${messageHeader.alertDetails.templateId}",
       |       "data": {},
       |       "alertFrom": "${messageHeader.alertDetails.alertFrom}"
       |   }
       | }
       | """.stripMargin
  }

  private def hash(fields: Seq[String]): String = {
    val sha256Digester = MessageDigest.getInstance("SHA-256")
    Base64.encodeBase64String(sha256Digester.digest(fields.mkString("/").getBytes("UTF-8")))
  }
}

