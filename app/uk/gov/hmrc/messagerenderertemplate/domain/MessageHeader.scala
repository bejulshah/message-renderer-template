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
import uk.gov.hmrc.time.DateTimeUtils

final case class MessageHeader(recipient: TaxEntity,
                               subject: String,
                               statutory: Option[Boolean],
                               alertDetails: AlertDetails,
                               validFrom: LocalDate = DateTimeUtils.now.toLocalDate) {

}

final case class AlertDetails(templateId: String, data: Map[String, String], alertFrom: LocalDate)

final case class TaxEntity(regime: String, identifier: TaxIdWithName)
