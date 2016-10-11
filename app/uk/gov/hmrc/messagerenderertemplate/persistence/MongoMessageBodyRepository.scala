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

package uk.gov.hmrc.messagerenderertemplate.persistence

import play.api.Logger
import reactivemongo.api.DB
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.messagerenderertemplate.domain.{BodyNotFound, MessageBody, MessageBodyId, MessageBodyRepository}
import uk.gov.hmrc.messagerenderertemplate.persistence.model.MessageBodyPersistenceModel
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

class MongoMessageBodyRepository(implicit mongo: () => DB)
  extends ReactiveRepository[MessageBodyPersistenceModel, BSONObjectID](
    "messageBodies",
    mongo,
    MessageBodyPersistenceModel.formats,
    ReactiveMongoFormats.objectIdFormats
  ) with MessageBodyRepository {

  override def addNewMessageBodyWith(taxId: TaxIdWithName, subject: String, content: String)(implicit ec: ExecutionContext): Future[MessageBody] = {
    val messageBodyPersistenceModel = MessageBodyPersistenceModel.createNewWith(taxId, subject, content)

    insert(messageBodyPersistenceModel).map { (result: WriteResult) =>
      messageBodyPersistenceModel.toMessageBody()
    }
  }

  override def findBy(id: MessageBodyId)(implicit ec: ExecutionContext): Future[Either[BodyNotFound.type, MessageBody]] =
    Future.fromTry(BSONObjectID.parse(id.value)).flatMap { bsonID =>
      findById(bsonID).map { result =>
        result match {

          case Some(model) => Right(model.toMessageBody())
          case None => Left(BodyNotFound)
        }
      }
    }
}
