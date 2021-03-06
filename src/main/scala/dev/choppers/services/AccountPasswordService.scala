package dev.choppers.services

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.osinka.i18n.Lang
import dev.choppers.email.client.{EmailClient, EmailTemplate}
import dev.choppers.model.api.AccountProtocol.{AccountPasswordReset, AccountPasswordUpdate}
import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.model.persistence.AccountPasswordTokenEntity.AccountPasswordTokenEntity
import dev.choppers.repositories.{AccountPasswordTokenRepository, AccountRepository}
import dev.choppers.services.AccountUpdatePasswordResult.AccountUpdatePasswordResult
import dev.choppers.services.ResetPasswordResult.ResetPasswordResult
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountPasswordService[E <: AccountEntity, T, ID] extends AccountPasswordSupport {

  val accountRepository: AccountRepository[E, ID]

  val accountPasswordTokenRepository: AccountPasswordTokenRepository

  val emailClient: EmailClient

  def passwordChangedEmail(account: T)(implicit lang: Lang): EmailTemplate

  def resetPasswordEmail(account: T, token: String)(implicit lang: Lang): EmailTemplate

  def updateAccountPassword(id: BSONObjectID, accountPasswordUpdate: AccountPasswordUpdate)
                           (implicit lang: Lang, transformEntity: E => T): Future[AccountUpdatePasswordResult] = {
    accountRepository.findById(id) flatMap {
      case Some(account) =>
        if (verifyPassword(accountPasswordUpdate.currentPassword, account))
          accountRepository.updatePassword(account._id, hashPassword(accountPasswordUpdate.newPassword))
            .map { _ =>
              emailClient.sendEmail(passwordChangedEmail(account))
              AccountUpdatePasswordResult.Success
            }
        else
          Future.successful(AccountUpdatePasswordResult.IncorrectPassword)
      case None => Future.successful(AccountUpdatePasswordResult.AccountIdNotFound)
    }
  }

  def generatePasswordToken(account: E)(implicit lang: Lang, transformEntity: E => T): Future[Unit] = {
    val token = UUID.randomUUID().toString
    accountPasswordTokenRepository.insert(
      AccountPasswordTokenEntity(accountId = account._id, token = token, expires = Instant.now.plus(1, ChronoUnit.DAYS))
    ) flatMap { _ =>
      emailClient.sendEmail(resetPasswordEmail(account, token))
    }
  }

  def resetPassword(accountPasswordReset: AccountPasswordReset): Future[ResetPasswordResult] = {
    accountPasswordTokenRepository.findByToken(accountPasswordReset.token) flatMap {
      case Some(t) =>
        if (t.expires isAfter Instant.now) {
          accountPasswordTokenRepository.deleteById(t._id)
          accountRepository.updatePassword(t.accountId, hashPassword(accountPasswordReset.password))
            .map(_ => ResetPasswordResult.Success)
        } else
          Future.successful(ResetPasswordResult.TokenExpired)
      case _ => Future.successful(ResetPasswordResult.TokenNotFound)
    }
  }
}

object AccountUpdatePasswordResult extends Enumeration {
  type AccountUpdatePasswordResult = Value
  val AccountIdNotFound, IncorrectPassword, Success = Value
}

object ResetPasswordResult extends Enumeration {
  type ResetPasswordResult = Value
  val TokenExpired, TokenNotFound, Success = Value
}
