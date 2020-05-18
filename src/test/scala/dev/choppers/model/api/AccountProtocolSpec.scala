package dev.choppers.model.api

import dev.choppers.model.api.AccountProtocol._
import org.specs2.mutable.Specification

class AccountProtocolSpec extends Specification {

  "AccountSignup creation" should {

    case class CustomerSignup(password: String) extends AccountSignup

    "Fail if password is not provided" in {
      CustomerSignup("") should throwA[IllegalArgumentException]
    }

    "Fail if password is less than 8 characters long" in {
      CustomerSignup("pass") should throwA[IllegalArgumentException]
    }
  }

  "AccountLogin creation" should {

    case class CustomerLogin(identifier: String, password: String, rememberMe: Boolean = false) extends AccountLogin[String]

    "Fail if password is not provided" in {
      CustomerLogin("test@email.com", "") should throwA[IllegalArgumentException]
    }
  }

  "AccountPasswordReset creation" should {
    "Fail if Current password is not provided" in {
      AccountPasswordUpdate("", "password") should throwA[IllegalArgumentException]
    }

    "Fail if New password is not provided" in {
      AccountPasswordUpdate("password", "") should throwA[IllegalArgumentException]
    }

    "Fail if New Password is less than 8 characters long" in {
      AccountPasswordUpdate("password", "pass") should throwA[IllegalArgumentException]
    }
  }

  "AccountPasswordReset creation" should {
    "Fail if Token is not provided" in {
      AccountPasswordReset("", "password") should throwA[IllegalArgumentException]
    }

    "Fail if Password is not provided" in {
      AccountPasswordReset("Token", "") should throwA[IllegalArgumentException]
    }

    "Fail if Password is less than 8 characters long" in {
      AccountPasswordReset("Token", "pass") should throwA[IllegalArgumentException]
    }
  }
}
