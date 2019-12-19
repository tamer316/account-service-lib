package dev.choppers.repositories

import java.time.Instant

import com.github.limansky.mongoquery.reactive._
import dev.choppers.model.persistence.AccountEntity._
import dev.choppers.mongo.{Mongo, Repository}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountRepository[T <: AccountEntity] extends Repository[T] {
  this: Mongo =>

  collection.flatMap(_.indexesManager.ensure(Index(Seq(("email", IndexType.Ascending)), Some("emailIndex"), true)))

  def findByEmail(email: String): Future[Option[T]] = findOne(mq"{email:$email}")

  def updatePassword(id: BSONObjectID, password: String) = {
    updateById(id, mq"{$$set:{password:$password}}")
  }

  def updateLock(id: BSONObjectID, locked: Boolean, lockExpires: Option[Instant] = None) = {
    val updateDoc = lockExpires match {
      case Some(l) => mq"{$$set:{locked:$locked, lockExpires:$lockExpires}}"
      case None => mq"{$$set:{locked:$locked}, $$unset:{lockedExpires:1}}"
    }
    updateById(id, updateDoc)
  }

  def updateLastLoggedIn(id: BSONObjectID, lastLoggedIn: Instant = Instant.now) = {
    updateById(id, mq"""{$$set:{lastLoggedIn: ${lastLoggedIn}}}""")
  }
}
