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
import org.joda.time.DateTimeUtils
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, LoneElement}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS
import play.api.test.{FakeApplication, FakeRequest, TestServer}
import reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.messagerenderertemplate.acceptance.microservices.{AuthServiceMock, MessageServiceMock}
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.it.Port
import uk.gov.hmrc.play.it.UrlHelper.-/
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Random

class CreateMessageSpec extends UnitSpec
  with WithFakeApplication
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar
  with MongoSpecSupport
  with LoneElement
  with TableDrivenPropertyChecks {

  override def beforeAll() = {
    super.beforeAll()
    appServer.start()
    auth.start()
    messageService.start()
  }

  override def afterAll() = {
    super.afterAll()
    appServer.stop()
    auth.stop()
    messageService.stop()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    DateTimeUtils.setCurrentMillisFixed(1000L)
    await(mongo().collection[JSONCollection]("messageBodies").remove(Json.obj()))
  }

  override protected def afterEach() = {
    super.afterEach()
    messageService.reset()
    auth.reset()
    DateTimeUtils.setCurrentMillisSystem()
  }

  private val appPort = Port.randomAvailable

  val auth = new AuthServiceMock()
  val messageService = new MessageServiceMock(auth.token)

  val random = new Random

  def randomUtr = SaUtr(random.nextInt(1000000).toString)

  def randomNino = {
    val prefix = Nino.validPrefixes(random.nextInt(Nino.validPrefixes.length))
    val number = random.nextInt(1000000)
    val suffix = Nino.validSuffixes(random.nextInt(Nino.validSuffixes.length))
    Nino(f"$prefix$number%06d$suffix")
  }

  def messageHeaderFor(taxId: TaxIdWithName,
                       regime: String,
                       statutory: Option[Boolean] = Some(true)) = {
    MessageHeader(
      Recipient(regime, taxId),
      s"Message for recipient: $regime - ${taxId.value}",
      hash = "messageHash",
      alertDetails = AlertDetails(
        templateId = "template1234",
        data = Map(
          "alertData1" -> "alertValue1",
          "alertData2" -> "alertValue2"
        )
      ),
      statutory = statutory
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

  val appServer = TestServer(appPort, app)

  val fakeGetRequest = FakeRequest("GET", "/").withHeaders((HttpHeaders.AUTHORIZATION, auth.token))
  val fakePostRequest = FakeRequest("POST", "/").withHeaders((HttpHeaders.AUTHORIZATION, auth.token))

  def messageCreationRequestFor(message: MessageHeader): String = {
    val json = Json.obj(
      "regime" -> message.recipient.regime,
      "taxId" -> Json.obj(
        "name" -> message.recipient.taxId.name,
        "value" -> message.recipient.taxId.value
      )
    ) ++ message.statutory.fold(Json.obj())(s => Json.obj("statutory" -> s))
    json.toString
  }

  def appPath(path: String) = {
    s"http://localhost:$appPort/message-renderer-template/${-/(path)}"
  }

  def callCreateMessageWith(creationRequest: String) = {
    WS.url(appPath(s"/messages")).
      withHeaders(
        (HttpHeaders.AUTHORIZATION, auth.token),
        (HttpHeaders.CONTENT_TYPE, "application/json")
      ).
      post(creationRequest)
  }

  val messageHeaders = Table(
    "messageHeaders",
    messageHeaderFor(randomUtr, "sa", statutory = Some(true)),
    messageHeaderFor(randomUtr, "sa", statutory = Some(false)),
    messageHeaderFor(randomNino, "paye", statutory = None)
  )

  "POST /messages" should {
    forAll(messageHeaders) { (messageHeader) =>
      s"return 201 if the messageHeader is newly created for ${messageHeader.recipient.taxId.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.successfullyCreates(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        response.futureValue.status shouldBe Status.CREATED
        messageService.receivedMessageCreateRequestFor(messageHeader)
      }

      s"return 200 if the messageHeader has been already created before for ${messageHeader.recipient.taxId.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.returnsDuplicateExistsFor(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        response.futureValue.status shouldBe Status.OK
        messageService.receivedMessageCreateRequestFor(messageHeader)
      }

      s"stores the messageHeader body in its own collection ${messageHeader.recipient.taxId.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.successfullyCreates(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        response.futureValue.status shouldBe Status.CREATED
        val messageBodyId = (response.futureValue.json \ "message" \ "body" \ "id").toString

        val messageBodies = mongo().collection[JSONCollection]("messageBodies").
          find(Json.obj()).
          cursor[JsValue]().
          collect[Seq]()

        messageBodies.futureValue.loneElement shouldBe Json.obj(
          "_id" -> messageBodyId,
          "content" -> "<div>This is a generated message.</div>"
        )
      }
    }
  }
}
