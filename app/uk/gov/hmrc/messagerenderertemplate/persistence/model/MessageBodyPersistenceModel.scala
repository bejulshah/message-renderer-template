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

package uk.gov.hmrc.messagerenderertemplate.persistence.model

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain.TaxIds
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.messagerenderertemplate.domain.{MessageBody, MessageBodyId}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.time.DateTimeUtils

case class MessageBodyPersistenceModel(_id: BSONObjectID,
                                       content: String,
                                       taxId: TaxIdWithName,
                                       createdAt: DateTime) {
  def toMessageBody(): MessageBody = {
    MessageBody(
      id = MessageBodyId(_id.stringify),
      taxId = taxId,
      content = content
    )
  }
}

object MessageBodyPersistenceModel {
  def createNewWith(taxId: TaxIdWithName, content: String) = {
    MessageBodyPersistenceModel(
      _id = BSONObjectID.generate,
      content = content,
      taxId = taxId,
      createdAt = DateTimeUtils.now
    )
  }

  implicit val dateReads: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val objectIdFormats: Format[BSONObjectID] = ReactiveMongoFormats.objectIdFormats

  implicit val taxIdFormats = new Format[TaxIdWithName] {
    override def reads(json: JsValue): JsResult[TaxIdWithName] = {
      TaxIds.defaultSerialisableIds.toList.find(_.taxIdName == (json \ "name").as[String]).
        map(t => JsSuccess(t.build((json \ "value").as[String]))).
        getOrElse(JsError(s"unsupported identifier in ${Json.stringify(json)}"))
    }

    override def writes(taxId: TaxIdWithName): JsValue = JsObject(Seq("name" -> JsString(taxId.name), "value" -> JsString(taxId.value)))
  }

  implicit val formats: Format[MessageBodyPersistenceModel] = Json.format[MessageBodyPersistenceModel]
}
