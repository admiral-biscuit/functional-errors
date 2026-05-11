// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private data class SomeFailure(override val message: String, override val cause: Cause? = null) :
  Failure(message, cause)

private data class OtherFailure(override val message: String, override val cause: Cause? = null) :
  Failure(message, cause)

class FailureTest :
  FunSpec({
    val throwable1 = Throwable("Biscuit ate everything")
    val throwable2 = IllegalStateException("there is no food", throwable1)
    val failure1 = SomeFailure("I am hungry", ThrowableCause(throwable2))
    val failure2 = OtherFailure("I am dying", FailureCause(failure1))

    context("nested failure") {
      test("causal chain") {
        failure2.causalChain() shouldBe listOf(FailureCause(failure1), ThrowableCause(throwable2))
        failure2.causalChain(stopAtFirstThrowable = false) shouldBe
          listOf(FailureCause(failure1), ThrowableCause(throwable2), ThrowableCause(throwable1))
      }

      test("root cause") {
        failure2.rootCause() shouldBe ThrowableCause(throwable2)
        failure2.rootCause(stopAtFirstThrowable = false) shouldBe ThrowableCause(throwable1)
      }
    }

    context("string representation") {
      test("toSimpleString includes creation site") {
        val failure = SomeFailure("oops")

        failure.toSimpleString() shouldBe "SomeFailure: oops\n\tat ${failure.createdAt}"
      }

      test("toSimpleString without creation site omits location") {
        val failure = object : Failure("oops", createdAt = null) {}
        failure.toSimpleString() shouldBe "${failure.javaClass.simpleName}: oops"
      }

      test("toPrettyString") {
        val prettyString = failure2.toPrettyString()

        // Failure lines include the creation-site location suffix; check substrings only.
        prettyString shouldContain "OtherFailure: I am dying"
        prettyString shouldContain "\nCaused by: SomeFailure: I am hungry"
        prettyString shouldContain "\nCaused by: java.lang.IllegalStateException: there is no food"
        prettyString shouldContain "Caused by: java.lang.Throwable: Biscuit ate everything"
      }

      test("toPrettyString with non-default arguments") {
        val failureToString = { failure: Failure ->
          "FAILURE ${failure.javaClass.simpleName}: ${failure.message}"
        }

        val throwableToString = { throwable: Throwable ->
          "THROWABLE ${throwable.javaClass.simpleName}: ${throwable.message}"
        }

        val joinStrings = { strings: List<String> -> strings.joinToString(separator = "\n-> ") }

        val prettyString =
          failure2.toPrettyString(
            failureToString = failureToString,
            throwableToString = throwableToString,
            joinStrings = joinStrings,
            stopAtFirstThrowable = false,
          )

        prettyString shouldBe
          """
          FAILURE OtherFailure: I am dying
          -> FAILURE SomeFailure: I am hungry
          -> THROWABLE IllegalStateException: there is no food
          -> THROWABLE Throwable: Biscuit ate everything
          """
            .trimIndent()
      }
    }

    context("causeFailure") {
      test("right passes through") {
        "o.k.".right().causeFailure("Hi!", ::SomeFailure).shouldBeRight("o.k.")
      }

      test("left causes left") {
        val otherFailure =
          SomeFailure("Hi!").left().causeFailure("Bye!", ::OtherFailure).shouldBeLeft()
        otherFailure shouldBe OtherFailure("Bye!", FailureCause(SomeFailure("Hi!")))
      }
    }

    context("catchAndCauseFailure") {
      test("non-throwing code returns right") {
        val result =
          catchAndCauseFailure(message = "Hi!", transformation = ::SomeFailure) { "o.k." }
        result shouldBeRight "o.k."
      }

      test("throwing code returns left failure") {
        val failure =
          catchAndCauseFailure(message = "Hi!", transformation = ::SomeFailure) {
              throw Throwable("Oh no!")
            }
            .shouldBeLeft()

        failure.message shouldBe "Hi!"
        failure.cause.shouldBeInstanceOf<ThrowableCause>().throwable.message shouldBe "Oh no!"
      }

      test("suspended non-throwing code returns right") {
        val result =
          suspendCatchAndCauseFailure(message = "Hi!", transformation = ::SomeFailure) { "o.k." }
        result shouldBeRight "o.k."
      }

      test("suspended throwing code returns left failure") {
        val failure =
          suspendCatchAndCauseFailure(message = "Hi!", transformation = ::SomeFailure) {
              throw Throwable("Oh no!")
            }
            .shouldBeLeft()

        failure.message shouldBe "Hi!"
        failure.cause.shouldBeInstanceOf<ThrowableCause>().throwable.message shouldBe "Oh no!"
      }
    }

    test("toCause") {
      val throwable = Throwable("Hi!")
      throwable.toCause() shouldBe ThrowableCause(throwable)

      val failure = SomeFailure("Hi!")
      failure.toCause() shouldBe FailureCause(failure)
    }
  })
