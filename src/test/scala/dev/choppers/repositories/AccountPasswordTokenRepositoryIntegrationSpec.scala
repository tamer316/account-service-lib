package dev.choppers.repositories

import java.time.Instant

import dev.choppers.model.persistence.AccountPasswordTokenEntity._
import dev.choppers.mongo.EmbeddedMongoSpecification
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._

class AccountPasswordTokenRepositoryIntegrationSpec extends Specification with EmbeddedMongoSpecification {

  private trait Context extends Scope {
    val accountId = BSONObjectID.parse("589d9c8e2c00002d00b1142d").get

    val passToken1 = AccountPasswordTokenEntity(
      accountId = accountId,
      token = "Token1",
      expires = Instant.now
    )

    val passToken2 = AccountPasswordTokenEntity(
      accountId = accountId,
      token = "Token2",
      expires = Instant.now
    )

    val accountPasswordTokenRepository: AccountPasswordTokenRepository = new AccountPasswordTokenRepository with TestMongo {
      override val collectionName = "customers_password_tokens"
    }
  }

  "findByToken" should {
    "Find by Token" in new Context {
      Await.result(accountPasswordTokenRepository.insert(passToken1), 5 seconds)
      Await.result(accountPasswordTokenRepository.insert(passToken2), 5 seconds)

      val res = Await.result(accountPasswordTokenRepository.findByToken("Token1"), 5 seconds)

      res mustEqual Some(passToken1)
    }

    "Return None if no Token matches" in new Context {
      Await.result(accountPasswordTokenRepository.insert(passToken1), 5 seconds)
      Await.result(accountPasswordTokenRepository.insert(passToken2), 5 seconds)

      val res = Await.result(accountPasswordTokenRepository.findByToken("Token3"), 5 seconds)

      res mustEqual None
    }
  }
}
