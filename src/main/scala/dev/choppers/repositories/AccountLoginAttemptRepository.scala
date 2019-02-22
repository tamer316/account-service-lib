package dev.choppers.repositories

import java.time.Instant

import com.github.limansky.mongoquery.reactive._
import dev.choppers.model.persistence.AccountLoginAttemptEntity.AccountLoginAttemptEntity
import dev.choppers.mongo.{Mongo, Repository}
import reactivemongo.bson._

import scala.concurrent.Future

trait AccountLoginAttemptRepository extends Repository[AccountLoginAttemptEntity] {
  this: Mongo =>

  implicit val reader: BSONDocumentReader[AccountLoginAttemptEntity] = Macros.reader[AccountLoginAttemptEntity]

  implicit val writer: BSONDocumentWriter[AccountLoginAttemptEntity] = Macros.writer[AccountLoginAttemptEntity]

  def findByAccount(accountId: BSONObjectID, fromDate: Instant): Future[Seq[AccountLoginAttemptEntity]] =
    find(mqt"{accountId:$accountId, createdDate:{$$gte : $fromDate}}"[AccountLoginAttemptEntity])

  def deleteByAccount(accountId: BSONObjectID) = delete(mqt"{accountId : $accountId}"[AccountLoginAttemptEntity])
}