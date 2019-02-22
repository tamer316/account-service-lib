package dev.choppers.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.model.persistence.AccountLoginAttemptEntity.AccountLoginAttemptEntity
import dev.choppers.repositories.{AccountLoginAttemptRepository, AccountRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountLoginSupport[T <: AccountEntity] extends AccountPasswordSupport {

  val unsuccessfulLoginAttemptsBeforeLock: Int
  val unsuccessfulLoginAttemptsHourThreshold: Int

  def accountLoginAttemptRepository: AccountLoginAttemptRepository

  def accountRepository: AccountRepository[T]

  protected def successfulLogin(account: AccountEntity): Future[Unit] = {
    accountLoginAttemptRepository.deleteByAccount(account._id)
    accountRepository.updateLock(account._id, false)
    accountRepository.updateLastLoggedIn(account._id)
  }

  protected def unsuccessfulLogin(account: AccountEntity): Future[Unit] = {
    accountLoginAttemptRepository.insert(AccountLoginAttemptEntity(accountId = account._id)) map { _ =>
      accountLoginAttemptRepository.findByAccount(account._id,
        Instant.now.minus(unsuccessfulLoginAttemptsHourThreshold, ChronoUnit.HOURS)) map { loginAttempts =>
        loginAttempts.size match {
          case x if x >= unsuccessfulLoginAttemptsBeforeLock =>
            accountRepository.updateLock(account._id, true,
              Some(Instant.now.plus(unsuccessfulLoginAttemptsHourThreshold, ChronoUnit.HOURS)))
          case _ =>
        }
      }
    }
  }

  protected def checkIfAccountLocked(account: AccountEntity): Boolean = {
    if (account.locked)
      account.lockExpires match {
        case Some(i) => i isAfter Instant.now
        case None => true
      }
    else
      false
  }
}
