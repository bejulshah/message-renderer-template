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

import play.api.Play
import play.api.mvc._
import play.libs.Json
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.messagerenderertemplate.connectors.MessageConnector
import uk.gov.hmrc.messagerenderertemplate.controllers.model.MessageCreationRequest
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

object MessageRendererController extends MessageRendererController {
  override def messageRepository: MessageRepository = new MessageConnector
}

trait MessageRendererController extends BaseController {

  def messageRepository: MessageRepository

  def createNewMessage(regime: String, taxIdentifier: String) = Action.async { implicit request =>
    messageRepository.add(Message(
      Recipient(regime, taxIdFor(regime, taxIdentifier)),
      subject = s"Message for recipient: $regime - $taxIdentifier",
      hash = "messageHash",
      alertDetails = AlertDetails(templateId = "bla", data = Map()),
      statutory = statutoryFor(regime)
    )).map {
      case MessageAdded() => Created("")
      case DuplicateMessage() => Ok("")
    }
  }

  def newMessage() = Action.async(parse.json) {
    implicit request =>
      withJsonBody[MessageCreationRequest] { messageCreationRequest =>
        messageRepository.add(messageCreationRequest.generateMessage()).
          map {
            case MessageAdded() => Created("")
            case DuplicateMessage() => Ok("")
          }
      }
  }

  def statutoryFor(regime: String): Option[Boolean] = {
    regime match {
      case "sa" => Some(true)
      case "paye" => None
    }
  }


  def taxIdFor(regime: String, taxIdentifier: String) = {
    regime match {
      case "sa" => SaUtr(taxIdentifier)
      case "paye" => Nino(taxIdentifier)
    }
  }

  def renderMessage(regime: String,
                    taxIdentifier: String,
                    messageId: String) = Action.async { implicit request =>
    Future.successful(Ok("Hello world"))
  }
}
