package dev.choppers.model.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import dev.choppers.akka.http.json.JavaDateTimeJsonProtocol
import spray.json.DefaultJsonProtocol

object AccountProtocol extends Validators {

  trait Account {
    val id: String
    val email: String
  }

  trait AccountSignup {
    var email: String
    var password: String

    email = email.trim.toLowerCase
    password = password.trim
    requireEmail("Email", email)
    requirePassword("Password", password)
  }

  case class AccountLogin(var email: String, var password: String, rememberMe: Boolean = false) {
    email = email.trim.toLowerCase
    password = password.trim

    requireEmail("Email", email)
    requireNonEmptyText("Password", password)
  }

  case class AccountPasswordUpdate(var currentPassword: String, var newPassword: String) {
    currentPassword = currentPassword.trim
    newPassword = newPassword.trim

    requireNonEmptyText("Current password", currentPassword)
    requirePassword("New password", newPassword)
  }

  case class AccountForgotPassword(var email: String) {
    email = email.trim.toLowerCase

    requireEmail("Email", email)
  }

  case class AccountPasswordReset(var token: String, var password: String) {
    token = token.trim
    password = password.trim

    requireNonEmptyText("Token", token)
    requirePassword("New password", password)
  }

}

trait AccountJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol with JavaDateTimeJsonProtocol {

  import AccountProtocol._

  implicit val accountLoginFormat = jsonFormat3(AccountLogin.apply)
  implicit val accountPasswordUpdateFormat = jsonFormat2(AccountPasswordUpdate.apply)
  implicit val accountForgotPasswordFormat = jsonFormat1(AccountForgotPassword.apply)
  implicit val accountPasswordResetFormat = jsonFormat2(AccountPasswordReset.apply)
}