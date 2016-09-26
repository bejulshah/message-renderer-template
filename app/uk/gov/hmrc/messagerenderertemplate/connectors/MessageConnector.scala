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
import play.api.libs.json.Json
import uk.gov.hmrc.messagerenderertemplate.WSHttp
import uk.gov.hmrc.messagerenderertemplate.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, Upstream4xxResponse}

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class MessageConnector extends MessageRepository with ServicesConfig {
  def http: HttpGet with HttpPost = WSHttp

  val messageBaseUrl: String = baseUrl("message")

  override def add(message: Message)(implicit hc: HeaderCarrier): Future[AddingResult] = {
    http.POST(s"$messageBaseUrl/messages", Json.obj())
      .map { response =>
        response.status match {
          case Status.OK => MessageAdded()
        }
      }.recover {
      case Upstream4xxResponse(message, Status.CONFLICT, _, _) => DuplicateMessage()
    }
  }
}

