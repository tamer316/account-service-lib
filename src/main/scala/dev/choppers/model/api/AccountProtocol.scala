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
    val password: String

    requirePassword("Password", password)
  }

  trait AccountLogin[ID] {
    def identifier: ID
    val password: String
    val rememberMe: Boolean

    requireNonEmptyText("Password", password)
  }

  case class AccountPasswordUpdate(currentPassword: String, newPassword: String) {
    requireNonEmptyText("Current password", currentPassword)
    requirePassword("New password", newPassword)
  }

  trait AccountForgotPassword[ID] {
    def identifier: ID
  }

  case class AccountPasswordReset(var token: String, password: String) {
    token = token.trim

    requireNonEmptyText("Token", token)
    requirePassword("New password", password)
  }

}

trait AccountJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol with JavaDateTimeJsonProtocol {

  import AccountProtocol._

  implicit val accountPasswordUpdateFormat = jsonFormat2(AccountPasswordUpdate.apply)
  implicit val accountPasswordResetFormat = jsonFormat2(AccountPasswordReset.apply)
}
