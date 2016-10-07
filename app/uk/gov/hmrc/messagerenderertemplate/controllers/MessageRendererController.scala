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

import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.messagerenderertemplate.connectors.{MessageHeaderServiceConnector, MicroserviceAuthConnector}
import uk.gov.hmrc.messagerenderertemplate.controllers.model.MessageCreationRequest
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.messagerenderertemplate.persistence.MongoMessageBodyRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.DateTimeUtils

object MessageRendererController extends MessageRendererController {

  override lazy val messageHeaderRepository: MessageHeaderRepository = new MessageHeaderServiceConnector

  override lazy val messageBodyRepository: MessageBodyRepository = MongoMessageBodyRepository

  override lazy val authConnector: MicroserviceAuthConnector = MicroserviceAuthConnector
}

trait MessageRendererController extends BaseController {

  def messageHeaderRepository: MessageHeaderRepository

  def messageBodyRepository: MessageBodyRepository

  def authConnector: MicroserviceAuthConnector

  def newMessage() = Action.async(parse.json) {
    implicit request =>
      withJsonBody[MessageCreationRequest] { messageCreationRequest =>
        val newMessageHeader = messageCreationRequest.generateMessage()

        messageBodyRepository.addNewMessageBodyWith( newMessageHeader.recipient.identifier,
          content =
            s"""<h2>${newMessageHeader.subject}</h2>
                |<div>Created at ${DateTimeUtils.now.toString()}</div>
                |<div>This is a message that has been generated for user from ${newMessageHeader.recipient.regime}
                |with ${newMessageHeader.recipient.identifier.name} value of ${newMessageHeader.recipient.identifier.value}.
                |</div>""".stripMargin.replaceAll("\n", " ")
        ).
          flatMap { messageBody =>
            messageHeaderRepository.add(newMessageHeader, messageBody).
              map {
                case MessageAdded => Created(responseWith(messageBody.id))
                case DuplicateMessage => Ok(responseWith(messageBody.id))
              }
          }
      }
  }

  def render(id: MessageBodyId) = Action.async { implicit request =>
    for {
      messageBody <- messageBodyRepository.findBy(id)
      currentTaxIds <- authConnector.currentTaxIdentifiers
    } yield {
      messageBody match {
        case Left(_) => NotFound
        case Right(message) => currentTaxIds.find(_ == message.taxId).map(_ => Ok(message.content)).getOrElse(Unauthorized)
      }
    }
  }

  private def responseWith(messageBodyId: MessageBodyId) = Json.obj(
    "message" -> Json.obj(
      "body" -> Json.obj(
        "id" -> messageBodyId.value
      )
    )
  )
}
