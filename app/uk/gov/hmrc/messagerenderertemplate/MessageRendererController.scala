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

import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.messagerenderertemplate.connectors.MessageConnector
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.microservice.controller.BaseController

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

object MessageRendererController extends MessageRendererController {
  override def messageRepository: MessageRepository = new MessageConnector
}

trait MessageRendererController extends BaseController {

  def messageRepository: MessageRepository

  def createNewMessage(regime: String, taxIdentifier: String) = Action.async { implicit request =>
    messageRepository.add(Message(
      Recipient(regime, SaUtr(taxIdentifier)),
      subject = s"Message for recipient: sa - $taxIdentifier",
      hash = "messageHash"
    )).map {
      case MessageAdded() => Created("")
      case DuplicateMessage() => Ok("")
    }
  }

  def renderMessage(regime: String,
                    taxIdentifier: String,
                    messageId: String) = Action.async { implicit request =>
    Future.successful(Ok("Hello world"))
  }
}
