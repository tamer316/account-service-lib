package dev.choppers.model.persistence

import java.time.Instant

import reactivemongo.bson._

object AccountLoginAttemptEntity {

  case class AccountLoginAttemptEntity(_id: BSONObjectID = BSONObjectID.generate,
                                       accountId: BSONObjectID,
                                       createdDate: Instant = Instant.now)

}
