package dev.choppers.services

import java.time.Instant

import com.osinka.i18n.Lang
import dev.choppers.model.api.AccountProtocol._
import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.model.persistence.AccountLoginAttemptEntity.AccountLoginAttemptEntity
import dev.choppers.repositories._
import org.mindrot.jbcrypt.BCrypt
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AccountServiceSpec extends Specification with Mockito {

  private trait Context extends Scope {
    implicit def toCustomer(customerEntity: CustomerEntity): Customer = {
      Customer(customerEntity._id.stringify, customerEntity.email)
    }

    case class Customer(id: String, email: String) extends Account

    case class CustomerEntity(_id: BSONObjectID = BSONObjectID.generate,
                              email: String,
                              password: String,
                              createdDate: Instant = Instant.now,
                              lastLoggedIn: Instant = Instant.now,
                              enabled: Boolean = true,
                              locked: Boolean = false,
                              lockExpires: Option[Instant] = None) extends AccountEntity

    class CustomerRepository extends AccountRepository[CustomerEntity] {
      val db = mock[Future[DefaultDB]]

      val collectionName = "customers"

      implicit val reader: BSONDocumentReader[CustomerEntity] = Macros.reader[CustomerEntity]

      implicit val writer: BSONDocumentWriter[CustomerEntity] = Macros.writer[CustomerEntity]
    }

    class CustomerLoginAttemptRepository extends AccountLoginAttemptRepository {
      val db = mock[Future[DefaultDB]]

      val collectionName = "customers_login_attempts"
    }

    class CustomerService(val accountRepository: CustomerRepository,
                          val accountLoginAttemptRepository: AccountLoginAttemptRepository) extends AccountService[CustomerEntity, Customer] {
      val unsuccessfulLoginAttemptsBeforeLock: Int = 3
      val unsuccessfulLoginAttemptsHourThreshold: Int = 3
    }

    implicit val lang = Lang("en")
    val customerId = BSONObjectID.parse("589d9c8e2c00002d00b1142d").get
    val customerRepository = mock[CustomerRepository]
    val customerLoginAttemptRepository = mock[CustomerLoginAttemptRepository]
    val customerService = new CustomerService(customerRepository, customerLoginAttemptRepository)
  }

  "findById" should {
    "Return None if customer not found" in new Context {
      customerRepository.findById(customerId.stringify) returns Future.successful(None)

      val res = Await.result(customerService.findById(customerId.stringify), 5 seconds)
      res mustEqual None

      there was one(customerRepository).findById(customerId.stringify)
    }

    "Return customer" in new Context {
      val customerEntity = CustomerEntity(
        email = "test@email.com",
        password = "password"
      )
      val customer = Customer(customerEntity._id.stringify, "test@email.com")

      customerRepository.findById(customerId.stringify) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.findById(customerId.stringify), 5 seconds)
      res mustEqual Some(customer)

      there was one(customerRepository).findById(customerId.stringify)
    }
  }

  "authenticate" should {
    "Return Failure if customer not found" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      customerRepository.findByEmail(customerLogin.email) returns Future.successful(None)

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.IncorrectLogin)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      there were noMoreCallsTo(customerRepository)
    }

    "Return Failure if password is incorrect" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns customerId
      customerEntity.enabled returns true
      customerEntity.locked returns false
      customerEntity.password returns BCrypt.hashpw("password1", BCrypt.gensalt())

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))
      customerLoginAttemptRepository.insert(any[AccountLoginAttemptEntity]) returns Future.successful({})
      customerLoginAttemptRepository.findByAccount(Matchers.eq(customerId), any[Instant]) returns Future.successful(Nil)

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.IncorrectLogin)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      eventually(there was one(customerLoginAttemptRepository).insert(any[AccountLoginAttemptEntity]))
      eventually(there was one(customerLoginAttemptRepository).findByAccount(Matchers.eq(customerId), any[Instant]))
      there were noMoreCallsTo(customerRepository)
      there were noMoreCallsTo(customerLoginAttemptRepository)
    }

    "Return Failure and lock account if password is incorrect and 3 attempted logins have occurred" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns customerId
      customerEntity.enabled returns true
      customerEntity.locked returns false
      customerEntity.password returns BCrypt.hashpw("password1", BCrypt.gensalt())

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))
      customerLoginAttemptRepository.insert(any[AccountLoginAttemptEntity]) returns Future.successful({})
      val mockedLoginAttempts = mock[Seq[AccountLoginAttemptEntity]]
      mockedLoginAttempts.size returns 3
      customerLoginAttemptRepository.findByAccount(Matchers.eq(customerId), any[Instant]) returns Future.successful(mockedLoginAttempts)

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.IncorrectLogin)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      eventually(there was one(customerLoginAttemptRepository).insert(any[AccountLoginAttemptEntity]))
      eventually(there was one(customerLoginAttemptRepository).findByAccount(Matchers.eq(customerId), any[Instant]))
      eventually(there was one(customerRepository).updateLock(Matchers.eq(customerId), Matchers.eq(true), any[Some[Instant]]))
      there were noMoreCallsTo(customerRepository)
      there were noMoreCallsTo(customerLoginAttemptRepository)
    }

    "Return Failure if account is locked and expiry is in the future" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns customerId
      customerEntity.enabled returns true
      customerEntity.locked returns true
      customerEntity.lockExpires returns Some(Instant.now.plusSeconds(100))

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.AccountLocked)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      there were noMoreCallsTo(customerRepository)
      there were noCallsTo(customerLoginAttemptRepository)
    }

    "Return Failure if account is locked and expiry is not set" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns customerId
      customerEntity.enabled returns true
      customerEntity.locked returns true
      customerEntity.lockExpires returns None

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.AccountLocked)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      there were noMoreCallsTo(customerRepository)
      there were noCallsTo(customerLoginAttemptRepository)
    }

    "Successfully Log customer in if Account locked but expiry has passed" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = CustomerEntity(
        email = "test@email.com",
        password = BCrypt.hashpw("password", BCrypt.gensalt()),
        locked = true,
        lockExpires = Some(Instant.now.minusSeconds(100))
      )
      val customer = Customer(customerEntity._id.stringify, "test@email.com")

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Right(customer)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
    }

    "Return Failure if account is disabled" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns customerId
      customerEntity.enabled returns false

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Left(AccountLoginError.AccountDisabled)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      there were noMoreCallsTo(customerRepository)
      there were noCallsTo(customerLoginAttemptRepository)
    }

    "Successfully Log customer in" in new Context {
      val customerLogin = AccountLogin("test@email.com", "password")
      val customerEntity = CustomerEntity(
        email = "test@email.com",
        password = BCrypt.hashpw("password", BCrypt.gensalt())

      )
      val customer = Customer(customerEntity._id.stringify, "test@email.com")

      customerRepository.findByEmail(customerLogin.email) returns Future.successful(Some(customerEntity))

      val res = Await.result(customerService.authenticate(customerLogin), 5 seconds)
      res mustEqual Right(customer)

      eventually(there was one(customerRepository).findByEmail(customerLogin.email))
      eventually(there was one(customerLoginAttemptRepository).deleteByAccount(customerEntity._id))
      eventually(there was one(customerRepository).updateLock(customerEntity._id, false))
      eventually(there was one(customerRepository).updateLastLoggedIn(customerEntity._id))
    }
  }
}
