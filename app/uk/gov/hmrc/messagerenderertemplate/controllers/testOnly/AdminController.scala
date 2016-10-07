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

package uk.gov.hmrc.messagerenderertemplate.controllers.testOnly

import play.api.mvc.Action
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.messagerenderertemplate.persistence.MongoMessageBodyRepository
import uk.gov.hmrc.messagerenderertemplate.persistence.model.MessageBodyPersistenceModel
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

trait AdminController  extends BaseController {
  def repo: ReactiveRepository[MessageBodyPersistenceModel, BSONObjectID]

  def deleteAll = Action.async { implicit request =>
    repo.removeAll().map(_ => Ok)
  }
}

object AdminController extends AdminController {
  lazy val repo = MongoMessageBodyRepository
}
