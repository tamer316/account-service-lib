package dev.choppers.model.api

import dev.choppers.model.api.AccountProtocol._
import org.specs2.mutable.Specification

class AccountProtocolSpec extends Specification {

  "AccountSignup creation" should {

    case class CustomerSignup(var email: String, var password: String) extends AccountSignup

    "Fail if Email is invalid" in {
      CustomerSignup("email", "password") should throwA[IllegalArgumentException]
    }

    "Fail if Email is not provided" in {
      CustomerSignup("", "password") should throwA[IllegalArgumentException]
    }

    "Fail if password is not provided" in {
      CustomerSignup("test@email.com", "") should throwA[IllegalArgumentException]
    }

    "Fail if password is less than 8 characters long" in {
      CustomerSignup("test@email.com", "pass") should throwA[IllegalArgumentException]
    }
  }

  "AccountLogin creation" should {
    "Fail if Email is invalid" in {
      AccountLogin("email", "password") should throwA[IllegalArgumentException]
    }

    "Fail if Email is not provided" in {
      AccountLogin("", "password") should throwA[IllegalArgumentException]
    }

    "Fail if password is not provided" in {
      AccountLogin("test@email.com", "") should throwA[IllegalArgumentException]
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

  "CustomerForgotPassword creation" should {
    "Fail if Email is invalid" in {
      AccountForgotPassword("email") should throwA[IllegalArgumentException]
    }

    "Fail if Email is not provided" in {
      AccountForgotPassword("") should throwA[IllegalArgumentException]
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
