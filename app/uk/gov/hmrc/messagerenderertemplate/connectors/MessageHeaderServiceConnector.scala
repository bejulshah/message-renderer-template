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

import play.api.http.Status
import uk.gov.hmrc.messagerenderertemplate.WSHttp
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, Upstream4xxResponse}

import scala.concurrent.Future


class MessageHeaderServiceConnector extends MessageHeaderRepository with ServicesConfig {
  def http: HttpGet with HttpPost = WSHttp

  val messageBaseUrl: String = baseUrl("message")

  import play.api.libs.json._

  private implicit val messageWrites: Writes[MessageHeader] = new Writes[MessageHeader] {
    override def writes(messageHeader: MessageHeader) = {
      Json.obj(
        "recipient" -> Json.obj(
          "regime" -> messageHeader.recipient.regime,
          "identifier" -> Json.obj(
            messageHeader.recipient.taxId.name -> messageHeader.recipient.taxId.value
          )
        ),
        "subject" -> messageHeader.subject,
        "hash" -> messageHeader.hash
      ) ++ messageHeader.statutory.fold(Json.obj())(s => Json.obj("statutory" -> s))
    }
  }

  override def add(messageHeader: MessageHeader)(implicit hc: HeaderCarrier): Future[AddingResult] = {
    http.POST(s"$messageBaseUrl/messages", messageHeader).
      map { response =>
        response.status match {
          case Status.OK => MessageAdded
        }
      }.
      recover {
        case Upstream4xxResponse(_, Status.CONFLICT, _, _) => DuplicateMessage
      }
  }
}

