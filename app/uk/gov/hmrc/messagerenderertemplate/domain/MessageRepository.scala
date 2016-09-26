package uk.gov.hmrc.messagerenderertemplate.domain

import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

sealed trait AddingResult

final case class MessageAdded() extends AddingResult
final case class DuplicateMessage() extends AddingResult

trait MessageRepository {

  def add(message: Message)(implicit hc: HeaderCarrier): Future[AddingResult]

}
