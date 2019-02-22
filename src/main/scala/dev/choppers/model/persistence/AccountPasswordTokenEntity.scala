package dev.choppers.model.persistence

import java.time.Instant

import reactivemongo.bson._

object AccountPasswordTokenEntity {

  case class AccountPasswordTokenEntity(_id: BSONObjectID = BSONObjectID.generate,
                                        accountId: BSONObjectID,
                                        token: String,
                                        expires: Instant)

}
