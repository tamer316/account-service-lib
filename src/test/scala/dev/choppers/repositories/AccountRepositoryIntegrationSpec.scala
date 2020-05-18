package dev.choppers.repositories

import java.time.Instant

import com.github.limansky.mongoquery.reactive._
import dev.choppers.model.persistence.AccountEntity._
import dev.choppers.mongo.EmbeddedMongoSpecification
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class AccountRepositoryIntegrationSpec extends Specification with EmbeddedMongoSpecification {

  private trait Context extends Scope {

    case class CustomerEntity(_id: BSONObjectID = BSONObjectID.generate,
                              email: String,
                              password: String,
                              createdDate: Instant = Instant.now,
                              lastLoggedIn: Instant = Instant.now,
                              enabled: Boolean = true,
                              locked: Boolean = false,
                              lockExpires: Option[Instant] = None) extends AccountEntity

    class CustomerRepository extends AccountRepository[CustomerEntity, String] with TestMongo {
      val collectionName = "customers"

      implicit val reader: BSONDocumentReader[CustomerEntity] = Macros.reader[CustomerEntity]

      implicit val writer: BSONDocumentWriter[CustomerEntity] = Macros.writer[CustomerEntity]

      override def findByIdentifier(identifier: String): Future[Option[CustomerEntity]] = findOne(mq"{email:$identifier}")
    }

    val account1 = CustomerEntity(
      email = "test1@email.com",
      password = "password"
    )

    val account2 = CustomerEntity(
      email = "test2@email.com",
      password = "password"
    )

    val customerRepository: CustomerRepository = new CustomerRepository
  }

  "findByEmail" should {
    "Find by email" in new Context {
      Await.result(customerRepository.insert(account1), 5 seconds)
      Await.result(customerRepository.insert(account2), 5 seconds)

      val res = Await.result(customerRepository.findByIdentifier("test1@email.com"), 5 seconds)

      res mustEqual Some(account1)
    }

    "Return None if email not found" in new Context {
      Await.result(customerRepository.insert(account1), 5 seconds)
      Await.result(customerRepository.insert(account2), 5 seconds)

      val res = Await.result(customerRepository.findByIdentifier("test3@email.com"), 5 seconds)

      res mustEqual None
    }
  }

  "updatePassword" should {
    "Update Customer Password" in new Context {
      Await.result(customerRepository.insert(account1), 5 seconds)
      Await.result(customerRepository.insert(account2), 5 seconds)

      Await.result(customerRepository.updatePassword(account2._id, "newPassword"), 5 seconds)

      val res = Await.result(customerRepository.findById(account2._id), 5 seconds).get
      res.password mustEqual "newPassword"

      val res2 = Await.result(customerRepository.findById(account1._id), 5 seconds)
      res2 mustEqual Some(account1)
    }
  }

  "updateLock" should {
    "Update Customer Locked and Lock Expiration" in new Context {
      Await.result(customerRepository.insert(account1), 5 seconds)
      Await.result(customerRepository.insert(account2), 5 seconds)

      val newLockExpiry = Instant.now.plusSeconds(100)

      Await.result(customerRepository.updateLock(account1._id, true, Some(newLockExpiry)), 5 seconds)

      val res = Await.result(customerRepository.findById(account1._id), 5 seconds).get
      res.locked mustEqual true
      res.lockExpires mustEqual Some(newLockExpiry)

      val res2 = Await.result(customerRepository.findById(account2._id), 5 seconds)
      res2 mustEqual Some(account2)
    }
  }

  "updateLastLoggedIn" should {
    "Update Customer Last Logged in" in new Context {
      Await.result(customerRepository.insert(account1), 5 seconds)
      Await.result(customerRepository.insert(account2), 5 seconds)

      val newLastLoggedIn = Instant.now.minusSeconds(100)

      Await.result(customerRepository.updateLastLoggedIn(account2._id, newLastLoggedIn), 5 seconds)

      val res = Await.result(customerRepository.findById(account2._id), 5 seconds).get
      res.lastLoggedIn mustEqual newLastLoggedIn

      val res2 = Await.result(customerRepository.findById(account1._id), 5 seconds)
      res2 mustEqual Some(account1)
    }
  }
}
