package dev.choppers.services

import dev.choppers.model.api.AccountProtocol._
import dev.choppers.model.persistence.AccountEntity.AccountEntity
import dev.choppers.services.AccountLoginError.AccountLoginError
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountService[E <: AccountEntity, T <: Account] extends AccountLoginSupport[E] with Logging {

  def findById(id: String)(implicit transformEntity: E => T): Future[Option[T]] = accountRepository.findById(id) map { c =>
    c.map { b => b }
  }

  def authenticate(accountLogin: AccountLogin)(implicit transformEntity: E => T): Future[Either[AccountLoginError, T]] = {
    accountRepository.findByEmail(accountLogin.email) map {
      case Some(account) =>
        if (account.enabled) {
          checkIfAccountLocked(account) match {
            case true => Left(AccountLoginError.AccountLocked)
            case false =>
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