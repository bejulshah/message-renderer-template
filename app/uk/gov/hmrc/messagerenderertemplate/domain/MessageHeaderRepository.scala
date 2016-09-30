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

import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

sealed trait AddingResult extends Product with Serializable

case object MessageAdded extends AddingResult
case object DuplicateMessage extends AddingResult

trait MessageHeaderRepository {

  def add(messageHeader: MessageHeader, messageBodyId: MessageBodyId)
         (implicit hc: HeaderCarrier): Future[AddingResult]

}
