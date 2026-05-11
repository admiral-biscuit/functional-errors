// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.examples

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.admiralbiscuit.functionalerrors.Cause
import io.github.admiralbiscuit.functionalerrors.Failure
import io.github.admiralbiscuit.functionalerrors.catchAndCauseFailure
import io.github.admiralbiscuit.functionalerrors.causeFailure
import io.github.admiralbiscuit.functionalerrors.examples.UserServiceFailure.Unexpected

data class DataBaseFailure(override val message: String, override val cause: Cause? = null) :
  Failure(message, cause)

sealed class UserServiceFailure(override val message: String, override val cause: Cause? = null) :
  Failure(message, cause) {
  data class Unexpected(override val message: String, override val cause: Cause) :
    UserServiceFailure(message, cause)

  data class UserNotFound(val id: Int) : UserServiceFailure("User $id not found")
}

data class User(val id: Int)

object UserRepository {
  fun getUserByIdThrowing(id: Int): User? =
    when (id) {
      13 -> error("connection to database not possible")
      37 -> null
      else -> User(id)
    }

  fun getUserByIdEither(id: Int): Either<DataBaseFailure, User?> =
    catchAndCauseFailure("connection to database not possible", ::DataBaseFailure) {
      getUserByIdThrowing(id)
    }
}

object UserService {
  fun getUserById(id: Int): Either<UserServiceFailure, User> = either {
    val userOrNull =
      UserRepository.getUserByIdEither(id)
        .causeFailure("failed to get user from repository", ::Unexpected)
        .bind()

    ensureNotNull(userOrNull) { UserServiceFailure.UserNotFound(id) }
  }
}

fun main() {
  println("=== exception root cause ===")
  UserService.getUserById(13).onLeft { println(it.toPrettyString()) }

  println("=== non-exception root cause ===")
  UserService.getUserById(37).onLeft { println(it.toPrettyString()) }
}
