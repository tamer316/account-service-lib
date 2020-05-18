package dev.choppers.repositories

import java.time.Instant

import com.github.limansky.mongoquery.reactive._
import dev.choppers.model.persistence.AccountEntity._
import dev.choppers.mongo.{Mongo, Repository}
import reactivemongo.bson._

import scala.concurrent.Future

trait AccountRepository[T <: AccountEntity, ID] extends Repository[T] {
  this: Mongo =>

  def findByIdentifier(identifier: ID): Future[Option[T]]

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
