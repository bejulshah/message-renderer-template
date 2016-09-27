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

import play.api.libs.json.{JsResult, Json}
import uk.gov.hmrc.play.test.UnitSpec

class MessageCreationRequestSpec extends UnitSpec {

  "message creation request" should {
    "be parsed from json" in {
      val messageCreationRequest: JsResult[MessageCreationRequest] = MessageCreationRequest.messageCreationReads.
        reads(Json.parse("""{"regime":"sa","taxId":{"name":"sautr","value":"419794"}}"""))

      messageCreationRequest.isSuccess shouldBe true
      messageCreationRequest.get.regime shouldBe "sa"
      messageCreationRequest.get.taxId.name shouldBe "sautr"
      messageCreationRequest.get.taxId.value shouldBe "419794"
    }
  }
}
