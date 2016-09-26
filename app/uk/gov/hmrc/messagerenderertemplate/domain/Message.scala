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

package uk.gov.hmrc.messagerenderertemplate.domain

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName

final case class Message(recipient: Recipient,
                         subject: String,
                         hash: String,
                         body: Map[String, String] = Map(),
                         alertDetails: AlertDetails = AlertDetails(),
                         statutory: Boolean = false,
                         validFrom: LocalDate = LocalDate.now) {

}

final case class AlertDetails(templateId: String = "newMessageAlert",
                              data: Map[String, String] = Map(),
                              alertFrom: LocalDate = LocalDate.now
                             )

final case class Recipient(regime: String, taxId: TaxIdWithName)
