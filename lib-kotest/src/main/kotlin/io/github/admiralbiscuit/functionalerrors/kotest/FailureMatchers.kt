// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Failure
import io.github.admiralbiscuit.functionalerrors.FailureCause
import io.github.admiralbiscuit.functionalerrors.ThrowableCause
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

private fun haveMessage(message: String) =
  Matcher<Failure> { failure ->
    MatcherResult(
      failure.message == message,
      { "Expected message \"$message\" but was \"${failure.message}\"" },
      { "Expected message to not be \"$message\"" },
    )
  }

private fun haveMessageContaining(substring: String) =
  Matcher<Failure> { failure ->
    MatcherResult(
      failure.message.contains(substring),
      { "Expected message to contain \"$substring\" but was \"${failure.message}\"" },
      { "Expected message to not contain \"$substring\" but was \"${failure.message}\"" },
    )
  }

private fun haveFailureCause(failure: Failure) =
  Matcher<Failure> { actual ->
    MatcherResult(
      actual.cause == FailureCause(failure),
      { "Expected cause to be FailureCause($failure) but was ${actual.cause}" },
      { "Expected cause to not be FailureCause($failure)" },
    )
  }

private fun haveThrowableCause(throwable: Throwable) =
  Matcher<Failure> { actual ->
    MatcherResult(
      actual.cause == ThrowableCause(throwable),
      { "Expected cause to be ThrowableCause($throwable) but was ${actual.cause}" },
      { "Expected cause to not be ThrowableCause($throwable)" },
    )
  }

infix fun Failure.shouldHaveMessage(message: String): Failure = apply {
  should(haveMessage(message))
}

fun Failure.shouldNotHaveMessage(message: String): Failure = apply {
  shouldNot(haveMessage(message))
}

infix fun Failure.shouldHaveMessageContaining(substring: String): Failure = apply {
  should(haveMessageContaining(substring))
}

/** Asserts that the direct cause is a [FailureCause] wrapping [failure]. */
infix fun Failure.shouldHaveFailureCause(failure: Failure): Failure = apply {
  should(haveFailureCause(failure))
}

/** Asserts that the direct cause is a [ThrowableCause] wrapping [throwable]. */
infix fun Failure.shouldHaveThrowableCause(throwable: Throwable): Failure = apply {
  should(haveThrowableCause(throwable))
}

/** Asserts that the direct cause is a [FailureCause] and returns it for further inspection. */
fun Failure.shouldHaveFailureCause(): FailureCause {
  val cause = this.cause
  if (cause !is FailureCause) error("Expected cause to be a FailureCause but was $cause")
  return cause
}

/** Asserts that the direct cause is a [ThrowableCause] and returns it for further inspection. */
fun Failure.shouldHaveThrowableCause(): ThrowableCause {
  val cause = this.cause
  if (cause !is ThrowableCause) error("Expected cause to be a ThrowableCause but was $cause")
  return cause
}
