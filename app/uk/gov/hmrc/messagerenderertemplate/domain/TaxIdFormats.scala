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

import play.api.libs.json._
import uk.gov.hmrc.domain.TaxIds
import uk.gov.hmrc.domain.TaxIds._
import play.api.libs.functional.syntax._

object TaxIdFormats {

  val formats = new Format[TaxIdWithName] {
    override def reads(json: JsValue): JsResult[TaxIdWithName] = {
      TaxIds.defaultSerialisableIds.toList.find(_.taxIdName == (json \ "name").as[String]).
        map(t => JsSuccess(t.build((json \ "value").as[String]))).
        getOrElse(JsError(s"unsupported identifier in ${Json.stringify(json)}"))
    }

    override def writes(taxId: TaxIdWithName): JsValue = JsObject(Seq("name" -> JsString(taxId.name), "value" -> JsString(taxId.value)))
  }
}
