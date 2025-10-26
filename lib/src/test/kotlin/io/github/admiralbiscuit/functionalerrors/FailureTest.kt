package io.github.admiralbiscuit.functionalerrors

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
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

      test("causes") {
        failure2.causes() shouldBe
          listOf(FailureCause(failure1), ThrowableCause(throwable2), ThrowableCause(throwable1))
      }

      test("root cause") { failure2.rootCause() shouldBe ThrowableCause(throwable1) }

      test("print") {
        failure2.print() shouldBe
          """
          OtherFailure: I am dying
          caused by SomeFailure: I am hungry
          caused by IllegalStateException: there is no food
          caused by Throwable: Biscuit ate everything
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
