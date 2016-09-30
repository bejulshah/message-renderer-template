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

package uk.gov.hmrc.messagerenderertemplate.controllers

import play.api.mvc.PathBindable
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.messagerenderertemplate.domain.MessageBodyId

object binders {
  implicit def messageBodyIdBinder: PathBindable[MessageBodyId] =
    new PathBindable[MessageBodyId] {
      type Result = Either[String, MessageBodyId]

      override def unbind(key: String, value: MessageBodyId): String = value.value

      override def bind(key: String, value: String): Either[String, MessageBodyId] =
        BSONObjectID.parse(value).toOption.
          fold[Result](Left(s"Invalid id format was provided: $value")) { _ => Right(MessageBodyId(value)) }
    }
}
