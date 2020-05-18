package dev.choppers.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.github.limansky.mongoquery.reactive._
import com.osinka.i18n.Lang
import dev.choppers.email.client.{EmailClient, EmailTemplate}
import dev.choppers.model.api.AccountProtocol.{AccountPasswordReset, AccountPasswordUpdate}
import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.model.persistence.AccountPasswordTokenEntity.AccountPasswordTokenEntity
import dev.choppers.repositories.{AccountPasswordTokenRepository, AccountRepository}
import org.mindrot.jbcrypt.BCrypt
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import reactivemongo.api.DefaultDB
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AccountPasswordServiceSpec extends Specification with Mockito {

  private trait Context extends Scope {
    implicit def toCustomer(customerEntity: CustomerEntity): Customer = {
      Customer(customerEntity._id.stringify, customerEntity.email)
    }

    case class Customer(id: String, email: String)

    case class CustomerEntity(_id: BSONObjectID = BSONObjectID.generate,
                              email: String,
                              password: String,
                              createdDate: Instant = Instant.now,
                              lastLoggedIn: Instant = Instant.now,
                              enabled: Boolean = true,
                              locked: Boolean = false,
                              lockExpires: Option[Instant] = None) extends AccountEntity

    class CustomerRepository extends AccountRepository[CustomerEntity, String] {
      val db = mock[Future[DefaultDB]]

      val collectionName = "customers"

      implicit val reader: BSONDocumentReader[CustomerEntity] = Macros.reader[CustomerEntity]

      implicit val writer: BSONDocumentWriter[CustomerEntity] = Macros.writer[CustomerEntity]

      override def findByIdentifier(identifier: String): Future[Option[CustomerEntity]] = findOne(mq"{email:$identifier}")
    }

    class CustomerPasswordService(val accountRepository: CustomerRepository,
                                  val accountPasswordTokenRepository: AccountPasswordTokenRepository,
                                  val emailClient: EmailClient) extends AccountPasswordService[CustomerEntity, Customer, String] {

      def passwordChangedEmail(account: Customer)(implicit lang: Lang): EmailTemplate = mock[EmailTemplate]

      def resetPasswordEmail(account: Customer, token: String)(implicit lang: Lang): EmailTemplate = mock[EmailTemplate]
    }

    class CustomerPasswordTokenRepository extends AccountPasswordTokenRepository {
      val db = mock[Future[DefaultDB]]

      val collectionName = "customers_password_tokens"
    }

    implicit val lang = Lang("en")
    val accountId = BSONObjectID.parse("589d9c8e2c00002d00b1142d").get
    val accountRepository = mock[CustomerRepository]
    val customerPasswordTokenRepository = mock[CustomerPasswordTokenRepository]
    val emailClient = mock[EmailClient]
    val accountPasswordService = new CustomerPasswordService(accountRepository, customerPasswordTokenRepository, emailClient)
  }

  "updateAccountPassword" should {
    "Return Failure if account not found" in new Context {
      val customerPassUpdate = AccountPasswordUpdate("password", "newPassword")
      accountRepository.findById(accountId) returns Future.successful(None)

      val res = Await.result(accountPasswordService.updateAccountPassword(accountId, customerPassUpdate), 5 seconds)
      res mustEqual AccountUpdatePasswordResult.AccountIdNotFound

      there was one(accountRepository).findById(accountId)
      there were noCallsTo(emailClient)
    }

    "Return Failure if current password is incorrect" in new Context {
      val customerPassUpdate = AccountPasswordUpdate("password", "newPassword")
      val customerEntity = mock[CustomerEntity]
      customerEntity.password returns BCrypt.hashpw("password1", BCrypt.gensalt())
      accountRepository.findById(accountId) returns Future.successful(Some(customerEntity))

      val res = Await.result(accountPasswordService.updateAccountPassword(accountId, customerPassUpdate), 5 seconds)
      res mustEqual AccountUpdatePasswordResult.IncorrectPassword

      there was one(accountRepository).findById(accountId)
      there were noCallsTo(emailClient)
    }

    "Update a account's password successfully" in new Context {
      val customerPassUpdate = AccountPasswordUpdate("password", "newPassword")
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns accountId
      customerEntity.password returns BCrypt.hashpw("password", BCrypt.gensalt())
      accountRepository.findById(accountId) returns Future.successful(Some(customerEntity))
      accountRepository.updatePassword(Matchers.eq(accountId), any[String]) returns Future.successful({})

      val res = Await.result(accountPasswordService.updateAccountPassword(accountId, customerPassUpdate), 5 seconds)
      res mustEqual AccountUpdatePasswordResult.Success

      there was one(accountRepository).findById(accountId)
      val passwordCaptor = capture[String]
      there was one(accountRepository).updatePassword(Matchers.eq(accountId), passwordCaptor.capture)
      BCrypt.checkpw("newPassword", passwordCaptor.value) must beTrue
      there were noMoreCallsTo(accountRepository)

      there was one(emailClient).sendEmail(any[EmailTemplate])
    }
  }

  "generatePasswordToken" should {
    "Return Failure if account email not found" in new Context {
      accountRepository.findByIdentifier("test@email.com") returns Future.successful(None)

      val res = Await.result(accountPasswordService.generatePasswordToken("test@email.com"), 5 seconds)
      res mustEqual GeneratePasswordTokenResult.EmailNotFound

      there was one(accountRepository).findByIdentifier("test@email.com")
      there were noCallsTo(emailClient)
    }

    "Generate a password reset Token successfully" in new Context {
      val customerEntity = mock[CustomerEntity]
      customerEntity._id returns accountId
      accountRepository.findByIdentifier("test@email.com") returns Future.successful(Some(customerEntity))
      customerPasswordTokenRepository.insert(any[AccountPasswordTokenEntity]) returns Future.successful({})
      emailClient.sendEmail(any[EmailTemplate]) returns Future.successful({})

      val res = Await.result(accountPasswordService.generatePasswordToken("test@email.com"), 5 seconds)
      res mustEqual GeneratePasswordTokenResult.Success

      there was one(accountRepository).findByIdentifier("test@email.com")

      val passResetTokenEntityCaptor = capture[AccountPasswordTokenEntity]
      there was one(customerPasswordTokenRepository).insert(passResetTokenEntityCaptor.capture)
      val passResetTokenEntity = passResetTokenEntityCaptor.value
      passResetTokenEntity.accountId mustEqual accountId
      passResetTokenEntity.expires isBefore Instant.now.plus(1, ChronoUnit.DAYS) must beTrue

      there was one(emailClient).sendEmail(any[EmailTemplate])
    }
  }

  "resetPassword" should {
    "Return Failure if Token is not found" in new Context {
      val accountPassReset = AccountPasswordReset("Token", "password")
      customerPasswordTokenRepository.findByToken("Token") returns Future.successful(None)

      val res = Await.result(accountPasswordService.resetPassword(accountPassReset), 5 seconds)
      res mustEqual ResetPasswordResult.TokenNotFound

      there was one(customerPasswordTokenRepository).findByToken("Token")
    }

    "Return Failure if Token has expired" in new Context {
      val customerPassReset = AccountPasswordReset("Token", "password")
      val passResetTokenEntity = mock[AccountPasswordTokenEntity]
      passResetTokenEntity.expires returns Instant.now.minusSeconds(100)
      customerPasswordTokenRepository.findByToken("Token") returns Future.successful(Some(passResetTokenEntity))

      val res = Await.result(accountPasswordService.resetPassword(customerPassReset), 5 seconds)
      res mustEqual ResetPasswordResult.TokenExpired

      there was one(customerPasswordTokenRepository).findByToken("Token")
    }

    "Reset password successfully and hash password" in new Context {
      val tokenId = BSONObjectID.parse("589d9c8e2c00003d00b1142d").get
      val customerPassReset = AccountPasswordReset("Token", "password")
      val passResetTokenEntity = mock[AccountPasswordTokenEntity]
      passResetTokenEntity._id returns tokenId
      passResetTokenEntity.accountId returns accountId
      passResetTokenEntity.expires returns Instant.now.plusSeconds(100)
      customerPasswordTokenRepository.findByToken("Token") returns Future.successful(Some(passResetTokenEntity))
      accountRepository.updatePassword(Matchers.eq(accountId), any[String]) returns Future.successful({})

      val res = Await.result(accountPasswordService.resetPassword(customerPassReset), 5 seconds)
      res mustEqual ResetPasswordResult.Success

      there was one(customerPasswordTokenRepository).findByToken("Token")
      there was one(customerPasswordTokenRepository).deleteById(tokenId)

      val passwordCaptor = capture[String]
      there was one(accountRepository).updatePassword(Matchers.eq(accountId), passwordCaptor.capture)
      BCrypt.checkpw("password", passwordCaptor.value) must beTrue
    }
  }
}
