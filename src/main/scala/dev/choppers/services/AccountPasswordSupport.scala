package dev.choppers.services

import dev.choppers.model.persistence.AccountEntity.AccountEntity
import org.mindrot.jbcrypt.BCrypt

trait AccountPasswordSupport {
  protected def hashPassword(password: String) = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }

  protected def verifyPassword(password: String, c: AccountEntity) = {
    BCrypt.checkpw(password, c.password)
  }
}
