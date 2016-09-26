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
import play.api.http.{HeaderNames, Status}
import uk.gov.hmrc.messagerenderertemplate.domain.Message

class MessageServiceMock(authToken: String, servicePort: Int = 8910)
  extends WiremockService("message", servicePort) {

  def successfullyCreates(message: Message): Unit = {
    service.register(post(urlEqualTo("/messages")).
      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().withStatus(Status.OK)))
  }

  def returnsDuplicateExistsFor(message: Message): Unit = {
    service.register(post(urlEqualTo("/messages")).
      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().withStatus(Status.CREATED)))
  }

  def rendered(message: Message) = {

    """
      | {
      |   "recipient" : {
      |     "regime" : "sa",
      |     "identifier" : {
      |       "sautr" : "1234567899"
      |     }
      |   },
      |   "subject" : "Message subject line",
      |   "body" : {
      |     "meta-data-1": "meta-data-1-value",
      |     "meta-data-2": "meta-data-2-value"
      |   },
      | "validFrom" : "2013-06-04",
      | "alertDetails" : {
      | "templateId": "messageTemplateId",
      | "data": {
      | "alertParam1": "value1",
      | "alertParam2": "value2"
      | },
      | "recipientName": {
      | "title": "Mr",
      | "forename": "Wile",
      | "secondForename": "E",
      | "surname": "Coyote",
      | "honours": "FAST"
      | },
      | "alertFrom": "2013-07-04"
      | },
      | "contentParameters": {
      | "amount" : "£123.24",
      | "other-param": "content value"
      | },
      | "statutory": true,
      | "hash": "newMessageHashValue",
      | "renderUrl": {
      | "service": "renderServiceName",
      | "url": "a/url/to/get/renderered/content/{messageId}"
      | }
      | }
    """.stripMargin
  }

  //  def getByIdReturns(message: UpstreamMessageResponse): Unit = {
  //    givenThat(get(urlEqualTo(s"/messages/${message.id}")).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().
  //        withBody(
  //          jsonRepresentationOf(message)
  //        )))
  //  }
  //
  //  def getByIdFailsWith(status: Int, body: String = "", messageId: MessageId): Unit = {
  //    givenThat(get(urlEqualTo(s"/messages/${messageId.value}")).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().
  //        withStatus(status).
  //        withBody(
  //          body
  //        )))
  //  }
  //
  //  def headersListReturns(messageHeaders: Seq[MessageHeader]): Unit = {
  //    givenThat(get(urlEqualTo(s"/messages")).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().
  //        withBody(
  //          jsonRepresentationOf(messageHeaders)
  //        )))
  //  }
  //
  //  def headersListFailsWith(status: Int, body: String = ""): Unit = {
  //    givenThat(get(urlEqualTo(s"/messages")).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().
  //        withStatus(status).
  //        withBody(
  //          body
  //        )))
  //  }
  //
  //  def markAsReadSucceedsFor(messageBody: UpstreamMessageResponse): Unit = {
  //    givenThat(post(urlEqualTo(messageBody.markAsReadUrl.get.url)).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().withStatus(200)))
  //  }
  //
  //  def legacyMarkAsReadSucceedsFor(messageBody: UpstreamMessageResponse, status: Int = 200): Unit = {
  //    givenThat(post(urlEqualTo(messageBody.markAsReadUrl.get.url)).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().withStatus(status).withBody(
  //        s"""
  //           | {
  //           |   "service": "${messageBody.renderUrl.service}",
  //           |   "url": "${messageBody.renderUrl.url}"
  //           | }
  //        """.stripMargin
  //      )))
  //  }
  //
  //  def markAsReadFailsWith(status: Int, messageBody: UpstreamMessageResponse): Unit = {
  //    givenThat(post(urlEqualTo(messageBody.markAsReadUrl.get.url)).
  //      withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)).
  //      willReturn(aResponse().withStatus(status)))
  //  }
  //
  //  def assertMarkAsReadHasBeenCalledFor(messageBody: UpstreamMessageResponse): Unit = {
  //    verify(postRequestedFor(urlEqualTo(messageBody.markAsReadUrl.get.url))
  //      .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)))
  //  }
  //
  //  def assertMarkAsReadHasNeverBeenCalledFor(messageBody: UpstreamMessageResponse): Unit = {
  //    verify(0, postRequestedFor(urlEqualTo(messageBody.markAsReadUrl.get.url))
  //      .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken)))
  //  }
  //
  //  def assertMarkAsReadHasNeverBeenCalled(): Unit = {
  //    verify(0, postRequestedFor(urlMatching("/*")))
  //  }
  //
  //  def bodyWith(id: String,
  //               renderUrl: ResourceActionLocation = ResourceActionLocation("sa-message-renderer", "/utr/render"),
  //               markAsReadUrl: Option[ResourceActionLocation] = None) = {
  //    UpstreamMessageResponse(id, renderUrl, markAsReadUrl)
  //  }
  //
  //  def bodyToBeMarkedAsReadWith(id: String) = {
  //    bodyWith(id = id, markAsReadUrl = Some(ResourceActionLocation("message", s"/messages/$id/read-time")))
  //  }
  //
  //  def headerWith(id: String,
  //                 subject: String = "message subject",
  //                 validFrom: LocalDate = new LocalDate(29348L),
  //                 readTime: Option[DateTime] = None,
  //                 sentInError: Boolean = false) = {
  //    MessageHeader(MessageId(id), subject, validFrom, readTime, sentInError)
  //  }
  //
  //  def jsonRepresentationOf(message: UpstreamMessageResponse) = {
  //    if (message.markAsReadUrl.isDefined) {
  //      s"""
  //         |    {
  //         |      "id": "${message.id}",
  //         |      "markAsReadUrl": {
  //         |         "service": "${message.markAsReadUrl.get.service}",
  //         |         "url": "${message.markAsReadUrl.get.url}"
  //         |      },
  //         |      "renderUrl": {
  //         |         "service": "${message.renderUrl.service}",
  //         |         "url": "${message.renderUrl.url}"
  //         |      }
  //         |    }
  //      """.stripMargin
  //    } else {
  //      s"""
  //         |    {
  //         |      "id": "${message.id}",
  //         |      "renderUrl": {
  //         |         "service": "${message.renderUrl.service}",
  //         |         "url": "${message.renderUrl.url}"
  //         |      }
  //         |    }
  //      """.stripMargin
  //    }
  //
  //  }
  //
  //  def jsonRepresentationOf(messageHeaders: Seq[MessageHeader]) = {
  //    s"""
  //       | {
  //       | "items":[
  //       |   ${messageHeaders.map(messageHeaderAsJson).mkString(",")}
  //       | ],
  //       | "count": {
  //       | "total":     ${messageHeaders.size},
  //       | "read":     ${messageHeaders.count(header => header.readTime.isDefined)}
  //       |}
  //       |
  //      }
  //      """.stripMargin
  //  }
  //
  //  private def messageHeaderAsJson(messageHeader: MessageHeader): String = {
  //    if (messageHeader.readTime.isDefined)
  //      s"""
  //         | {
  //         | "id": "${messageHeader.id.value}",
  //         | "subject": "${messageHeader.subject}",
  //         | "validFrom": "${messageHeader.validFrom}",
  //         | "readTime": "${messageHeader.readTime.get}",
  //         | "sentInError": ${messageHeader.sentInError}
  //         | }
  //      """.stripMargin
  //    else
  //      s"""
  //         | {
  //         | "id": "${messageHeader.id.value}",
  //         | "subject": "${messageHeader.subject}",
  //         | "validFrom": "${messageHeader.validFrom}",
  //         | "sentInError": ${messageHeader.sentInError}
  //         | }
  //      """.stripMargin
  //  }
}
