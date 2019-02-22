package dev.choppers.repositories

import com.github.limansky.mongoquery.reactive._
import dev.choppers.model.persistence.AccountPasswordTokenEntity.AccountPasswordTokenEntity
import dev.choppers.mongo.{Mongo, Repository}
import reactivemongo.bson._

import scala.concurrent.Future

trait AccountPasswordTokenRepository extends Repository[AccountPasswordTokenEntity] {
  this: Mongo =>

  implicit val reader: BSONDocumentReader[AccountPasswordTokenEntity] = Macros.reader[AccountPasswordTokenEntity]

  implicit val writer: BSONDocumentWriter[AccountPasswordTokenEntity] = Macros.writer[AccountPasswordTokenEntity]

  def findByToken(token: String): Future[Option[AccountPasswordTokenEntity]] =
    findOne(mqt"{token:$token}"[AccountPasswordTokenEntity])
}