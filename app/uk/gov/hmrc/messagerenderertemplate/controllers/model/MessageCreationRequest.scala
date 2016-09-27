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

package uk.gov.hmrc.messagerenderertemplate.controllers.model

import play.api.libs.json.{JsResult, _}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.messagerenderertemplate.domain.{AlertDetails, Message, Recipient}

final case class MessageCreationRequest(regime: String, taxId: TaxId) {

  def generateMessage() = Message(
    Recipient(regime, taxId.asDomainTaxId),
    subject = s"Message for recipient: $regime - ${taxId.value}",
    hash = "messageHash",
    alertDetails = AlertDetails(templateId = "bla", data = Map()),
    statutory = statutoryFor(regime)
  )

  def statutoryFor(regime: String): Option[Boolean] = {
    regime match {
      case "sa" => Some(true)
      case "paye" => None
    }
  }
}

final case class TaxId(name: String, value: String) {
  def asDomainTaxId = {
    name match {
      case "sautr" => SaUtr(value)
      case "nino" => Nino(value)
    }
  }
}

object MessageCreationRequest {

  implicit val messageCreationReads: Reads[MessageCreationRequest] = new Reads[MessageCreationRequest] {
    override def reads(json: JsValue): JsResult[MessageCreationRequest] = {
      val regime: String = (json \ "regime").as[String]
      val name = (json \ "taxId" \ "name").as[String]
      val value = (json \ "taxId" \ "value").as[String]

      JsSuccess(
        MessageCreationRequest(
          regime,
          TaxId(name, value)
        )
      )
    }
  }

}
