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

package uk.gov.hmrc.messagerenderertemplate.controllers

import org.apache.http.HttpHeaders
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Play
import play.api.http.Status
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.messagerenderertemplate.acceptance.microservices.MessageServiceMock
import uk.gov.hmrc.messagerenderertemplate.domain.{Message, MessageId, Recipient}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.util.Random


class MessageRendererControllerSpec extends UnitSpec
  with WithFakeApplication
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar {

  override def beforeAll() = {
    super.beforeAll()
    Play.start(app)
    messageService.start()
  }

  override def afterAll() = {
    super.afterAll()
    Play.stop()
    messageService.stop()
  }

  override protected def afterEach() = {
    super.afterEach()
    messageService.reset()
  }

  private val authToken = "authToken234"
  val messageService = new MessageServiceMock(authToken)

  val random = new Random
  
  def randomUtr = SaUtr(random.nextInt(1000000).toString)

  def utrMessageFor(utr: SaUtr) = {
    Message(
      Recipient("sa", utr),
      s"Message for recipient: sa - ${utr.value}",
      hash = "messageHash"
    )
  }

  object TestGlobal extends play.api.GlobalSettings

  implicit val app = FakeApplication(
    withGlobal = Some(TestGlobal),
    additionalConfiguration = Map(
      "appName" -> "application-name",
      "appUrl" -> "http://microservice-name.service",
      "auditing.enabled" -> "false"
    ) ++ messageService.configuration
  )


  val fakeGetRequest = FakeRequest("GET", "/").withHeaders((HttpHeaders.AUTHORIZATION, authToken))
  val fakePostRequest = FakeRequest("POST", "/").withHeaders((HttpHeaders.AUTHORIZATION, authToken))
  
  def messageRendererController = MessageRendererController

  "POST /:regime/:taxId/messages" should {
    "return 201 if the message is newly created" in {
      val taxId = randomUtr
      val regime = "sa"

      messageService.successfullyCreates(utrMessageFor(taxId))
      
      val result = messageRendererController.createNewMessage(regime, taxId.value)(fakePostRequest)
      
      status(result) shouldBe Status.CREATED
    }

    "return 200 if the message has been already created before" in {
      val taxId = randomUtr
      val regime = "sa"
      
      messageService.returnsDuplicateExistsFor(utrMessageFor(taxId))

      val result = messageRendererController.createNewMessage(regime, taxId.value)(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "call message with the correct parameters to create the message" in {

    }
  }

  "GET /:regime/:taxId/messages/:messageId" should {

    "render a utr message" in {
//      val taxId = SaUtr("")
//      val messageId = MessageId("92834")
//      val regime = "sa"
//      val result = messageRendererController.renderMessage(regime, taxId.toString, messageId.value)
    }

    "render a nino message" in {
//      val taxId = Nino("")
//      val messageId = MessageId("92834")
//      val regime = "sa"
//      val result = messageRendererController.renderMessage(regime, taxId.toString, messageId.value)
    }
  }
}
