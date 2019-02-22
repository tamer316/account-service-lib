package dev.choppers.repositories

import java.time.Instant

import dev.choppers.model.persistence.AccountLoginAttemptEntity._
import dev.choppers.mongo.EmbeddedMongoSpecification
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._

class AccountLoginAttemptRepositoryIntegrationSpec extends Specification with EmbeddedMongoSpecification {

  private trait Context extends Scope {
    val accountId1 = BSONObjectID.parse("589d9c8e2c00001d00b1142d").get
    val accountId2 = BSONObjectID.parse("589d9c8e2c00002d00b1142d").get

    val loginAttempt1 = AccountLoginAttemptEntity(
      accountId = accountId1,
      createdDate = Instant.now.minusSeconds(100)
    )
    val loginAttempt2 = AccountLoginAttemptEntity(
      accountId = accountId2,
      createdDate = Instant.now.minusSeconds(100)
    )

    val accountLoginAttemptRepository: AccountLoginAttemptRepository = new AccountLoginAttemptRepository with TestMongo {
      override val collectionName = "customers_login_attempts"
    }
  }

  "findByAccount" should {
    "Find all for Account from a certain date" in new Context {
      val loginAttempt3 = AccountLoginAttemptEntity(
        accountId = accountId1,
        createdDate = Instant.now.minusSeconds(200)
      )
      val loginAttempt4 = AccountLoginAttemptEntity(
        accountId = accountId1,
        createdDate = Instant.now.minusSeconds(50)
      )
      Await.result(accountLoginAttemptRepository.insert(loginAttempt1), 5 seconds)
      Await.result(accountLoginAttemptRepository.insert(loginAttempt2), 5 seconds)
      Await.result(accountLoginAttemptRepository.insert(loginAttempt3), 5 seconds)
      Await.result(accountLoginAttemptRepository.insert(loginAttempt4), 5 seconds)

      val res = Await.result(accountLoginAttemptRepository.findByAccount(accountId1, Instant.now.minusSeconds(150)), 5 seconds)

      res mustEqual Seq(loginAttempt1, loginAttempt4)
    }
  }

  "deleteByAccount" should {
    "Delete all for Account" in new Context {
      val loginAttempt3 = AccountLoginAttemptEntity(
        accountId = accountId1
      )
      Await.result(accountLoginAttemptRepository.insert(loginAttempt1), 5 seconds)
      Await.result(accountLoginAttemptRepository.insert(loginAttempt2), 5 seconds)
      Await.result(accountLoginAttemptRepository.insert(loginAttempt3), 5 seconds)

      Await.result(accountLoginAttemptRepository.deleteByAccount(accountId1), 5 seconds)

      val res = Await.result(accountLoginAttemptRepository.findAll(), 5 seconds)

      res mustEqual Seq(loginAttempt2)
    }
  }
}
