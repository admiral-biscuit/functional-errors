// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Cause
import io.github.admiralbiscuit.functionalerrors.Failure
import io.github.admiralbiscuit.functionalerrors.FailureCause
import io.github.admiralbiscuit.functionalerrors.ThrowableCause
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class SomeFailure(override val message: String, override val cause: Cause? = null) :
  Failure

private data class OtherFailure(override val message: String, override val cause: Cause? = null) :
  Failure

class FailureMatchersTest :
  FunSpec({
    val cause = SomeFailure("cause failure")
    val throwable = Throwable("cause throwable")

    context("shouldHaveMessage") {
      val failure = SomeFailure("something went wrong")

      test("passes for matching message") { failure shouldHaveMessage "something went wrong" }

      test("fails for non-matching message") {
        val error = shouldThrow<AssertionError> { failure shouldHaveMessage "something else" }
        error.message shouldBe
          "Expected message \"something else\" but was \"something went wrong\""
      }

      test("shouldNotHaveMessage passes for non-matching message") {
        failure.shouldNotHaveMessage("something else")
      }
    }

    context("shouldHaveMessageContaining") {
      val failure = SomeFailure("something went wrong")

      test("passes when message contains substring") {
        failure shouldHaveMessageContaining "went wrong"
      }

      test("fails when message does not contain substring") {
        shouldThrow<AssertionError> { failure shouldHaveMessageContaining "all good" }
      }
    }

    context("shouldHaveFailureCause(failure)") {
      val failure = OtherFailure("top failure", FailureCause(cause))

      test("passes for matching cause") { failure shouldHaveFailureCause cause }

      test("fails for non-matching cause") {
        shouldThrow<AssertionError> { failure shouldHaveFailureCause OtherFailure("wrong cause") }
      }
    }

    context("shouldHaveThrowableCause(throwable)") {
      val failure = SomeFailure("top failure", ThrowableCause(throwable))

      test("passes for matching cause") { failure shouldHaveThrowableCause throwable }

      test("fails for non-matching cause") {
        shouldThrow<AssertionError> {
          failure shouldHaveThrowableCause Throwable("wrong throwable")
        }
      }
    }

    context("shouldHaveFailureCause()") {
      test("returns the FailureCause when present") {
        val failure = OtherFailure("top failure", FailureCause(cause))
        failure.shouldHaveFailureCause() shouldBe cause
      }

      test("fails when cause is a ThrowableCause") {
        val failure = SomeFailure("top failure", ThrowableCause(throwable))
        shouldThrow<IllegalStateException> { failure.shouldHaveFailureCause() }
      }

      test("fails when cause is null") {
        shouldThrow<IllegalStateException> { SomeFailure("no cause").shouldHaveFailureCause() }
      }
    }

    context("shouldHaveThrowableCause()") {
      test("returns the ThrowableCause when present") {
        val failure = SomeFailure("top failure", ThrowableCause(throwable))
        failure.shouldHaveThrowableCause() shouldBe throwable
      }

      test("fails when cause is a FailureCause") {
        val failure = OtherFailure("top failure", FailureCause(cause))
        shouldThrow<IllegalStateException> { failure.shouldHaveThrowableCause() }
      }

      test("fails when cause is null") {
        shouldThrow<IllegalStateException> { SomeFailure("no cause").shouldHaveThrowableCause() }
      }
    }
  })
