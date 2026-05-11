// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors.examples

import io.github.admiralbiscuit.functionalerrors.kotest.shouldHaveFailureCause
import io.github.admiralbiscuit.functionalerrors.kotest.shouldHaveMessage
import io.github.admiralbiscuit.functionalerrors.kotest.shouldHaveThrowableCause
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExampleTest :
  FunSpec({
    test("chained assertions demonstration") {
      UserService.getUserById(13)
        .shouldBeLeft()
        .shouldHaveMessage("failed to get user from repository")
        .shouldHaveFailureCause()
        .shouldHaveMessage("connection to database not possible")
        .shouldHaveThrowableCause()
        .message
        .shouldBe("connection to database not possible")
    }
  })
