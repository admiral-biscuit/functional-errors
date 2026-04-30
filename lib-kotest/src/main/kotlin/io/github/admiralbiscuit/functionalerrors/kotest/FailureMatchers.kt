// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Failure
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

fun haveMessage(message: String) =
  Matcher<Failure> { failure ->
    MatcherResult(
      failure.message == message,
      { "Expected message \"$message\" but was \"${failure.message}\"" },
      { "Expected message to not be \"$message\"" },
    )
  }

fun Failure.shouldHaveMessage(message: String): Failure = apply { should(haveMessage(message)) }

fun Failure.shouldNotHaveMessage(message: String): Failure = apply {
  shouldNot(haveMessage(message))
}
