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
import uk.gov.hmrc.messagerenderertemplate.domain.{MessageBody, MessageBodyId}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.time.DateTimeUtils

case class MessageBodyPersistenceModel(id: BSONObjectID,
                                       content: String,
                                       createdAt: DateTime) {
  def toMessageBody(): MessageBody = {
    MessageBody(
      id = MessageBodyId(id.stringify),
      content = content
    )
  }
}

object MessageBodyPersistenceModel {
  def createNewWith(content: String) = {
    MessageBodyPersistenceModel(
      id = BSONObjectID.generate,
      content = content,
      createdAt = DateTimeUtils.now
    )
  }

  implicit val formats = ReactiveMongoFormats.mongoEntity({
    implicit val dateReads = ReactiveMongoFormats.dateTimeRead
    implicit val dateWrites = ReactiveMongoFormats.dateTimeWrite
    implicit val objectIdFormats = ReactiveMongoFormats.objectIdFormats

    implicit val reads = (
      (__ \ "_id").read[BSONObjectID] and
        (__ \ "body").read[String] and
        (__ \ "createdAt").read[DateTime]
      ) (MessageBodyPersistenceModel.apply _)

    Format(reads, Json.writes[MessageBodyPersistenceModel])
  })
}
