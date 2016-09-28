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

import scala.concurrent.Future

object MessageRendererController extends MessageRendererController {

  import play.api.Play.current
  import play.modules.reactivemongo.ReactiveMongoPlugin

  private implicit val connection = ReactiveMongoPlugin.mongoConnector.db

  override def messageHeaderRepository: MessageHeaderRepository = new MessageHeaderServiceConnector

  override def messageBodyRepository: MessageBodyRepository = new MongoMessageBodyRepository {
    def mongo = connection
  }
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
                case MessageAdded => Created(
                  Json.obj(
                    "message" -> Json.obj(
                      "body" -> Json.obj(
                        "id" -> messageBody.id.value
                      )
                    )
                  )
                )
                case DuplicateMessage => Ok("")
              }
          }
      }
  }

  def renderMessage(regime: String,
                    taxIdentifier: String,
                    messageId: String) = Action.async { implicit request =>
    Future.successful(Ok("Hello world"))
  }
}
