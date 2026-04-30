// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Failure
import io.github.admiralbiscuit.functionalerrors.FailureCause
import io.github.admiralbiscuit.functionalerrors.ThrowableCause
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

fun haveMessageContaining(substring: String) =
  Matcher<Failure> { failure ->
    MatcherResult(
      failure.message.contains(substring),
      { "Expected message to contain \"$substring\" but was \"${failure.message}\"" },
      { "Expected message to not contain \"$substring\" but was \"${failure.message}\"" },
    )
  }

fun haveFailureCause(failure: Failure) =
  Matcher<Failure> { actual ->
    MatcherResult(
      actual.cause == FailureCause(failure),
      { "Expected cause to be FailureCause($failure) but was ${actual.cause}" },
      { "Expected cause to not be FailureCause($failure)" },
    )
  }

fun haveThrowableCause(throwable: Throwable) =
  Matcher<Failure> { actual ->
    MatcherResult(
      actual.cause == ThrowableCause(throwable),
      { "Expected cause to be ThrowableCause($throwable) but was ${actual.cause}" },
      { "Expected cause to not be ThrowableCause($throwable)" },
    )
  }

fun Failure.shouldHaveMessage(message: String): Failure = apply { should(haveMessage(message)) }

fun Failure.shouldNotHaveMessage(message: String): Failure = apply {
  shouldNot(haveMessage(message))
}

fun Failure.shouldHaveMessageContaining(substring: String): Failure = apply {
  should(haveMessageContaining(substring))
}

fun Failure.shouldHaveFailureCause(failure: Failure): Failure = apply {
  should(haveFailureCause(failure))
}

fun Failure.shouldHaveThrowableCause(throwable: Throwable): Failure = apply {
  should(haveThrowableCause(throwable))
}

fun Failure.shouldHaveFailureCause(): FailureCause {
  val cause = this.cause
  if (cause !is FailureCause) error("Expected cause to be a FailureCause but was $cause")
  return cause
}

fun Failure.shouldHaveThrowableCause(): ThrowableCause {
  val cause = this.cause
  if (cause !is ThrowableCause) error("Expected cause to be a ThrowableCause but was $cause")
  return cause
}
