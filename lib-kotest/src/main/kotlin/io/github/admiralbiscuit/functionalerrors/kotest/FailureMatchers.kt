// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Failure
import io.github.admiralbiscuit.functionalerrors.FailureCause
import io.github.admiralbiscuit.functionalerrors.ThrowableCause
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf

// region Matcher
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

// endregion

// region assertions
infix fun <F : Failure> F.shouldHaveMessage(message: String): F = apply {
  should(haveMessage(message))
}

infix fun <F : Failure> F.shouldNotHaveMessage(message: String): F = apply {
  shouldNot(haveMessage(message))
}

infix fun <F : Failure> F.shouldHaveMessageContaining(substring: String): F = apply {
  should(haveMessageContaining(substring))
}

/** Asserts that the direct cause is a [FailureCause] wrapping [failure]. */
infix fun <F : Failure> F.shouldHaveFailureCause(failure: Failure): F = apply {
  should(haveFailureCause(failure))
}

/** Asserts that the direct cause is a [ThrowableCause] wrapping [throwable]. */
infix fun <F : Failure> F.shouldHaveThrowableCause(throwable: Throwable): F = apply {
  should(haveThrowableCause(throwable))
}

/**
 * Asserts that the direct cause is a [FailureCause] and returns the wrapped [Failure] for further
 * inspection.
 */
fun Failure.shouldHaveFailureCause(): Failure = cause.shouldBeInstanceOf<FailureCause<*>>().failure

/**
 * Asserts that the direct cause is a [FailureCause] wrapping an [F] and returns the wrapped [F] for
 * further inspection.
 */
inline fun <reified F : Failure> Failure.shouldHaveFailureCauseOfType(): F =
  shouldHaveFailureCause().shouldBeInstanceOf<F>()

/**
 * Asserts that the direct cause is a [ThrowableCause] and returns the wrapped [Throwable] for
 * further inspection.
 */
fun Failure.shouldHaveThrowableCause(): Throwable =
  cause.shouldBeInstanceOf<ThrowableCause>().throwable
// endregion
