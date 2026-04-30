// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.kotest

import io.github.admiralbiscuit.functionalerrors.Cause
import io.github.admiralbiscuit.functionalerrors.Failure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class SomeFailure(override val message: String, override val cause: Cause? = null) :
  Failure

class FailureMatchersTest :
  FunSpec({
    context("shouldHaveMessage") {
      val failure = SomeFailure("something went wrong")

      test("shouldHaveMessage passes for matching message") {
        failure.shouldHaveMessage("something went wrong")
      }

      test("shouldHaveMessage fails for non-matching message") {
        val error = shouldThrow<AssertionError> { failure.shouldHaveMessage("something else") }
        error.message shouldBe
          "Expected message \"something else\" but was \"something went wrong\""
      }

      test("shouldNotHaveMessage passes for non-matching message") {
        failure.shouldNotHaveMessage("something else")
      }
    }
  })
