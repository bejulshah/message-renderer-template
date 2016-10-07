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
import play.api.libs.json._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.domain.TaxIds._
import uk.gov.hmrc.messagerenderertemplate.WSHttp
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object MicroserviceAuthConnector extends MicroserviceAuthConnector with ServicesConfig {
  val http = WSHttp
  def authBaseUrl: String = baseUrl("auth")

  object ApiReads {

    import play.api.libs.functional.syntax._

    implicit val taxIdentifiersReads: Reads[Set[TaxIdWithName]] = (
      (__ \ "accounts" \ "sa" \ "utr").readNullable[SaUtr].orElse(Reads.pure(None)) and
        (__ \ "accounts" \ "paye" \ "nino").readNullable[Nino].orElse(Reads.pure(None))
      ) (toSet _)

    private def toSet(utr: Option[SaUtr], nino: Option[Nino]): Set[TaxIdWithName] =
      Set(utr, nino).flatten: Set[TaxIdWithName]
  }
}

trait MicroserviceAuthConnector extends AuthConnector {
  def http: WSGet

  def currentTaxIdentifiers(implicit hc: HeaderCarrier): Future[Set[TaxIdWithName]] = {
    import uk.gov.hmrc.messagerenderertemplate.connectors.MicroserviceAuthConnector.ApiReads.taxIdentifiersReads

    http.GET[Set[TaxIdWithName]](s"$authBaseUrl/auth/authority").recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == Status.UNAUTHORIZED => Set()
    }
  }
}
