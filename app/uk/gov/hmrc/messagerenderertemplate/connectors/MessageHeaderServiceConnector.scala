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

package uk.gov.hmrc.messagerenderertemplate.connectors

import java.security.MessageDigest

import org.apache.commons.codec.binary.Base64
import play.api.http.Status
import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.messagerenderertemplate.WSHttp
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, Upstream4xxResponse}
import uk.gov.hmrc.messagerenderertemplate.controllers.routes

import scala.concurrent.Future

case class RecipientBody(regime: String, identifier: TaxIdWithName)

case class RenderUrl(service: String, url: String)

case class MessageHeaderCreation(recipient: RecipientBody,
                                 subject: String,
                                 hash: String,
                                 renderUrl: RenderUrl,
                                 alertDetails: AlertDetails,
                                 statutory: Option[Boolean])

object MessageHeaderCreationFormats {

}

object MessageHeaderCreation {
  implicit val messageCreationFormat: Format[MessageHeaderCreation] = Json.format[MessageHeaderCreation]
}


class MessageHeaderServiceConnector extends MessageHeaderRepository with ServicesConfig {

  def http: HttpGet with HttpPost = WSHttp

  val messageBaseUrl: String = baseUrl("message")

  import play.api.libs.json._

  private implicit val messageWrites: Writes[MessageHeaderCreation] =
    new Writes[MessageHeaderCreation] {
      override def writes(message: MessageHeaderCreation) = {
        val (messageBody, messageHeader) = message
        Json.obj(
          "recipient" -> Json.obj(
            "regime" -> messageHeader.recipient.regime,
            "identifier" -> Json.obj(
              messageHeader.recipient.taxId.name -> messageHeader.recipient.taxId.value
            )
          ),
          "subject" -> messageHeader.subject,
          "hash" -> hash(Seq("message-renderer-template", messageHeader.subject, messageBody.content)),
          "renderUrl" -> Json.obj(
            "service" -> "message-renderer-template",
            "url" -> s"${routes.MessageRendererController.render(messageBody.id).url}"
          ),
          "alertDetails" -> Json.obj(
            "templateId" -> messageHeader.alertDetails.templateId,
            "alertFrom" -> messageHeader.alertDetails.alertFrom,
            "data" -> Json.obj()
          )
        ) ++ messageHeader.statutory.fold(Json.obj())(s => Json.obj("statutory" -> s))
      }
    }

  override def add(messageHeader: MessageHeader, messageBody: MessageBody)
                  (implicit hc: HeaderCarrier): Future[AddingResult] = {

    http.POST(s"$messageBaseUrl/messages", (messageBody, messageHeader)).
      map { response =>
        response.status match {
          case Status.OK => MessageAdded
        }
      }.
      recover {
        case Upstream4xxResponse(_, Status.CONFLICT, _, _) => DuplicateMessage
      }
  }

  private def hash(fields: Seq[String]): String = {
    val sha256Digester = MessageDigest.getInstance("SHA-256")
    Base64.encodeBase64String(sha256Digester.digest(fields.mkString("/").getBytes("UTF-8")))
  }
}

