package io.github.admiralbiscuit.functionalerrors

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data class SomeFailure(override val message: String, override val cause: Cause? = null) :
  Failure

private data class OtherFailure(override val message: String, override val cause: Cause? = null) :
  Failure

class FailureTest :
  FunSpec({
    context("nested failure") {
      val throwable1 = Throwable("Biscuit ate everything")
      val throwable2 = IllegalStateException("there is no food", throwable1)
      val failure1 = SomeFailure("I am hungry", ThrowableCause(throwable2))
      val failure2 = OtherFailure("I am dying", FailureCause(failure1))

      test("causal chain") {
        failure2.causalChain() shouldBe listOf(FailureCause(failure1), ThrowableCause(throwable2))
      }

      test("root cause") { failure2.rootCause() shouldBe ThrowableCause(throwable2) }

      test("toPrettyString") {
        val prettyStringLines = failure2.toPrettyString().split("\n")

        prettyStringLines shouldContainInOrder
          listOf(
            "OtherFailure: I am dying",
            "Caused by: SomeFailure: I am hungry",
            "Caused by: java.lang.IllegalStateException: there is no food",
            "Caused by: java.lang.Throwable: Biscuit ate everything",
          )
      }

      test("toPrettyString with non-default arguments") {
        val failureToString = { failure: Failure ->
          "FAILURE ${failure.javaClass.simpleName}: ${failure.message}"
        }

        val throwableToString = { throwable: Throwable ->
          "THROWABLE ${throwable.javaClass.simpleName}: ${throwable.message}"
        }

        val joinStrings = { strings: List<String> -> strings.joinToString(separator = "\n-> ") }

        val prettyString = failure2.toPrettyString(failureToString, throwableToString, joinStrings)

        prettyString shouldBe
          """
          FAILURE OtherFailure: I am dying
          -> FAILURE SomeFailure: I am hungry
          -> THROWABLE IllegalStateException: there is no food
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
    }
  })
