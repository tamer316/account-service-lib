package dev.choppers.model.persistence

import java.time.Instant

import reactivemongo.bson._

object AccountEntity {

  trait AccountEntity {
    val _id: BSONObjectID

    val password: String

    val createdDate: Instant

    val lastLoggedIn: Instant

    val enabled: Boolean

    val locked: Boolean

    val lockExpires: Option[Instant]
  }

}
