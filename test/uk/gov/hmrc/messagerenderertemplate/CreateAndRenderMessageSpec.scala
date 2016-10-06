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
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.test.{FakeApplication, FakeRequest, TestServer}
import reactivemongo.bson.BSONObjectID
import reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.http.test.ResponseMatchers.{status => statusOf, _}
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr}
import uk.gov.hmrc.messagerenderertemplate.acceptance.microservices.{AuthServiceMock, MessageServiceMock}
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.messagerenderertemplate.persistence.model.MessageBodyPersistenceModel
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.dateTimeFormats
import uk.gov.hmrc.play.it.Port
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class CreateAndRenderMessageSpec extends UnitSpec
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
    DateTimeUtils.setCurrentMillisFixed(fixedDateTime.getMillis)
    await(mongo().collection[JSONCollection]("messageBodies").drop())
  }

  override protected def afterEach() = {
    super.afterEach()
    messageService.reset()
    auth.reset()
    DateTimeUtils.setCurrentMillisSystem()
  }

  val fixedDateTime = time.DateTimeUtils.now
  private val appPort = Port.randomAvailable

  val auth = new AuthServiceMock()
  val messageService = new MessageServiceMock(auth.token)

  val random = new Random

  def randomUtr = SaUtr(random.nextInt(1000000).toString)

  def randomNino = new Generator(random).nextNino

  object TestGlobal extends play.api.GlobalSettings

  implicit val app = FakeApplication(
    withGlobal = Some(TestGlobal),
    additionalConfiguration = Map(
      "appName" -> "message-renderer-template",
      "appUrl" -> "http://microservice-name.service",
      "auditing.enabled" -> "false",
      "mongodb.uri" -> mongoUri
    ) ++ messageService.configuration
  )

  val appServer = TestServer(appPort, app)

  def messageHeaderFor(taxId: TaxIdWithName,
                       regime: String,
                       statutory: Option[Boolean] = Some(true)) = {
    MessageHeader(
      TaxEntity(regime, taxId),
      s"Auto generated test message",
      statutory = statutory,
      alertDetails = AlertDetails("newMessageAlert", data = Map(), alertFrom = time.DateTimeUtils.now.toLocalDate)
    )
  }

  def messageBodyFor(messageBodyId: String,
                     messageHeader: MessageHeader) = {
    MessageBody(
      id = MessageBodyId(messageBodyId),
      taxId = messageHeader.recipient.identifier,
      content =
        s"""<h2>${messageHeader.subject}</h2>
            |<div>Created at ${time.DateTimeUtils.now.toString()}</div>
            |<div>This is a message that has been generated for user from ${messageHeader.recipient.regime}
            |with ${messageHeader.recipient.identifier.name} value of ${messageHeader.recipient.identifier.value}.
            |</div>""".stripMargin.replaceAll("\n", " ")
    )
  }

  val fakeGetRequest = FakeRequest("GET", "/").withHeaders((HttpHeaders.AUTHORIZATION, auth.token))
  val fakePostRequest = FakeRequest("POST", "/").withHeaders((HttpHeaders.AUTHORIZATION, auth.token))

  def messageCreationRequestFor(message: MessageHeader): String = {
    val json = Json.obj(
      "regime" -> message.recipient.regime,
      "taxId" -> Json.obj(
        "name" -> message.recipient.identifier.name,
        "value" -> message.recipient.identifier.value
      )
    ) ++ message.statutory.fold(Json.obj())(s => Json.obj("statutory" -> s))
    json.toString
  }

  def appPath(path: String) = {
    s"http://localhost:$appPort/message-renderer-template$path"
  }

  def callCreateMessageWith(creationRequest: String) = {
    WS.url(appPath("/messages")).
      withHeaders(
        (HttpHeaders.CONTENT_TYPE, "application/json")
      ).
      post(creationRequest)
  }

  def getMessageBy(id: MessageBodyId) =
    WS.url(appPath(s"/messages/${id.value}")).
      withHeaders(HttpHeaders.AUTHORIZATION -> auth.token).
      get()

  val messageHeaders = Table(
    "messageHeaders",
    messageHeaderFor(randomUtr, "sa", statutory = Some(true)),
    messageHeaderFor(randomUtr, "sa", statutory = Some(false)),
    messageHeaderFor(randomNino, "paye", statutory = None)
  )

  import play.api.libs.json._

  "POST /messages" should {
    forAll(messageHeaders) { (messageHeader) =>
      s"return 201 if the messageHeader is newly created for ${messageHeader.recipient.identifier.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.successfullyCreates(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        response.futureValue.status shouldBe Status.CREATED
        val messageBodyId = (response.futureValue.json \ "message" \ "body" \ "id").asOpt[String]
        messageBodyId shouldBe defined
        messageService.receivedMessageCreateRequestFor(messageHeader, messageBodyFor(messageBodyId.get, messageHeader))
      }

      s"return 200 if the messageHeader has been already created before for ${messageHeader.recipient.identifier.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.returnsDuplicateExistsFor(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        val messageBodyId = (response.futureValue.json \ "message" \ "body" \ "id").asOpt[String]
        messageBodyId shouldBe defined
        messageService.receivedMessageCreateRequestFor(messageHeader, messageBodyFor(messageBodyId.get, messageHeader))
      }

      s"stores the messageHeader body in its own collection ${messageHeader.recipient.identifier.name} with statutory ${messageHeader.statutory.getOrElse("None")}" in {
        messageService.successfullyCreates(messageHeader)

        val response = callCreateMessageWith(
          messageCreationRequestFor(messageHeader)
        )

        response.futureValue.status shouldBe Status.CREATED

        val messageBodies = mongo().collection[JSONCollection]("messageBodies").
          find(Json.obj()).
          cursor[JsValue]().
          collect[Seq]()

        val messageBodyId = (response.futureValue.json \ "message" \ "body" \ "id").as[String]
        messageBodies.futureValue.loneElement shouldBe Json.obj(
          "_id" -> BSONObjectID(
            messageBodyId
          ),
          "taxId" -> Json.obj("name" -> JsString(messageHeader.recipient.identifier.name), "value" -> JsString(messageHeader.recipient.identifier.value)),
          "content" -> messageBodyFor(messageBodyId, messageHeader).content,
          "createdAt" -> fixedDateTime
        )
      }
    }
  }

  "GET /messages/:id" should {

    "render a message when provided a valid ID" in {
      val nino = randomNino
      val messageBody: MessageBody = messageBodyHasBeenPersistedWith(nino, "<div>this is an example content</div>")
      auth.succeedsFor(TaxEntity("paye", nino))

      getMessageBy(messageBody.id) should have(
        statusOf(200),
        body(messageBody.content)
      )
    }

    "return a 404 when provided a missing ID" in {
      auth.succeedsFor(TaxEntity("paye", randomNino))
      getMessageBy(MessageBodyId(BSONObjectID.generate.stringify)) should have(
        statusOf(404)
      )
    }

    "return a 400 when provided an invalid ID" in {
      getMessageBy(MessageBodyId("hello")) should have(
        statusOf(400)
      )
    }

  }

  def messageBodyHasBeenPersistedWith(taxId:TaxIdWithName, content: String): MessageBody = {
    val msg = MessageBodyPersistenceModel.createNewWith(taxId, content)
    await(
      mongo().collection[JSONCollection]("messageBodies").insert(Json.toJson(msg).as[JsObject])
    ).n shouldBe 1
    msg.toMessageBody()
  }
}
