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
import uk.gov.hmrc.messagerenderertemplate.connectors.MessageHeaderServiceConnector
import uk.gov.hmrc.messagerenderertemplate.controllers.model.MessageCreationRequest
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.messagerenderertemplate.persistence.MongoMessageBodyRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

object MessageRendererController extends MessageRendererController {

  override lazy val messageHeaderRepository: MessageHeaderRepository = new MessageHeaderServiceConnector

  override lazy val messageBodyRepository: MessageBodyRepository = MongoMessageBodyRepository
}

trait MessageRendererController extends BaseController {

  def messageHeaderRepository: MessageHeaderRepository

  def messageBodyRepository: MessageBodyRepository

  def newMessage() = Action.async(parse.json) {
    implicit request =>
      withJsonBody[MessageCreationRequest] { messageCreationRequest =>
        val newMessage = messageCreationRequest.generateMessage()

        messageBodyRepository.addNewMessageBodyWith(content = "<div>This is a generated message.</div>").
          flatMap { messageBody =>
            messageHeaderRepository.add(newMessage).
              map {
                case MessageAdded => Created(responseWith(messageBody.id))
                case DuplicateMessage => Ok(responseWith(messageBody.id))
              }
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
