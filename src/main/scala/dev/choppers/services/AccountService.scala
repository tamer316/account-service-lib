package dev.choppers.services

import dev.choppers.model.api.AccountProtocol._
import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.services.AccountLoginError.AccountLoginError
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountService[E <: AccountEntity, T, ID] extends AccountLoginSupport[E, ID] with Logging {

  def findById(id: String)(implicit transformEntity: E => T): Future[Option[T]] = accountRepository.findById(id) map { c =>
    c.map { b => b }
  }

  def authenticate(accountLogin: AccountLogin[ID])(implicit transformEntity: E => T): Future[Either[AccountLoginError, T]] = {
    accountRepository.findByIdentifier(accountLogin.identifier) map {
      case Some(account) =>
        if (account.enabled) {
          if (checkIfAccountLocked(account)) {
            Left(AccountLoginError.AccountLocked)
          } else {
            if (verifyPassword(accountLogin.password, account)) {
              successfulLogin(account)
              Right(account)
            } else {
              unsuccessfulLogin(account)
              Left(AccountLoginError.IncorrectLogin)
            }
          }
        } else {
          Left(AccountLoginError.AccountDisabled)
        }
      case _ => Left(AccountLoginError.IncorrectLogin)
    }
  }
}

object AccountLoginError extends Enumeration {
  type AccountLoginError = Value
  val AccountLocked, AccountDisabled, IncorrectLogin = Value
}
