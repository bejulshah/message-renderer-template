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
import org.joda.time.LocalDate
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.messagerenderertemplate.WSHttp
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, Upstream4xxResponse}
import uk.gov.hmrc.messagerenderertemplate.controllers.routes

import scala.concurrent.Future


case class RenderUrl(service: String, url: String)

case class MessageHeaderCreation(recipient: TaxEntity,
                                 subject: String,
                                 hash: String,
                                 renderUrl: RenderUrl,
                                 alertDetails: AlertDetails,
                                 validFrom: LocalDate,
                                 statutory: Option[Boolean])
object MessageHeaderCreation {
  def create(serviceName: String, header: MessageHeader, body: MessageBody) =
    MessageHeaderCreation(
      header.recipient,
      header.subject,
      hash(Seq(header.recipient.regime,
        header.recipient.identifier.name,
        header.recipient.identifier.value,
        serviceName, header.subject, body.content)),
      RenderUrl(serviceName, s"${routes.MessageRendererController.render(body.id).url}"),
      header.alertDetails,
      header.validFrom,
      header.statutory
    )

  private def hash(fields: Seq[String]): String = {
    val sha256Digester = MessageDigest.getInstance("SHA-256")
    Base64.encodeBase64String(sha256Digester.digest(fields.mkString("/").getBytes("UTF-8")))
  }

}
object MessageHeaderCreationFormats {

  implicit val alertDetailsFormat: Format[AlertDetails] = Json.format[AlertDetails]
  implicit val taxIdWithName: Writes[TaxIdWithName] =
    Writes[TaxIdWithName] { value => Json.obj(value.name -> value.value) }
  implicit val taxEntityFormat: Writes[TaxEntity] = Json.writes[TaxEntity]
  implicit val renderUrlFormat: Writes[RenderUrl] = Json.format[RenderUrl]
  implicit val messageCreationWrites: Writes[MessageHeaderCreation] = Json.writes[MessageHeaderCreation]
}

class MessageHeaderServiceConnector extends MessageHeaderRepository with ServicesConfig with AppName {
  import MessageHeaderCreationFormats._

  def http: HttpGet with HttpPost = WSHttp

  lazy val messageBaseUrl: String = baseUrl("message")

  override def add(messageHeader: MessageHeader, messageBody: MessageBody)
                  (implicit hc: HeaderCarrier): Future[AddingResult] =
    http.POST(s"$messageBaseUrl/messages", MessageHeaderCreation.create(appName, messageHeader, messageBody)).
      map { response =>
        response.status match {
          case Status.OK => MessageAdded
        }
      }.
      recover {
        case Upstream4xxResponse(_, Status.CONFLICT, _, _) => DuplicateMessage
      }
}

