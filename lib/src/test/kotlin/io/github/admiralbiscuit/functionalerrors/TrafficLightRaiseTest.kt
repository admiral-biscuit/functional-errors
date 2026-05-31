// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TrafficLightRaiseTest :
  FunSpec({
    context("trafficLight builder") {
      test("block returning a plain value produces Green") {
        trafficLight<Int, String, String> { 42 } shouldBe Green(42)
      }

      test("raise produces Red with no warnings") {
        trafficLight<Int, String, String> { raise("err") } shouldBe Red("err", emptyList())
      }

      test("warn then return produces Yellow") {
        trafficLight<Int, String, String> {
          warn("w1")
          1
        } shouldBe Yellow(1, nonEmptyListOf("w1"))
      }

      test("multiple warns accumulate in order") {
        trafficLight<Int, String, String> {
          warn("w1")
          warn("w2")
          warn("w3")
          42
        } shouldBe Yellow(42, nonEmptyListOf("w1", "w2", "w3"))
      }

      test("warn after raise never executes") {
        trafficLight<Int, String, String> {
          raise("err")
          @Suppress("UNREACHABLE_CODE") warn("never")
          @Suppress("UNREACHABLE_CODE") 0
        } shouldBe Red("err", emptyList())
      }

      test("warnings accumulated before raise are preserved in Red") {
        trafficLight<Int, String, String> {
          warn("w1")
          raise("err")
          @Suppress("UNREACHABLE_CODE") 0
        } shouldBe Red("err", listOf("w1"))
      }
    }

    context("bind") {
      test("bind on Green returns value") {
        trafficLight<Int, String, String> { Green(7).bind() } shouldBe Green(7)
      }

      test("bind on Yellow returns value and accumulates warnings") {
        trafficLight<Int, String, String> {
          val v = Yellow(7, nonEmptyListOf("w1")).bind()
          v + 1
        } shouldBe Yellow(8, nonEmptyListOf("w1"))
      }

      test("bind on Red short-circuits") {
        var reached = false
        trafficLight {
          Red<String, String>("err").bind()
          @Suppress("UNREACHABLE_CODE")
          reached = true
          @Suppress("UNREACHABLE_CODE") 0
        }
        reached shouldBe false
      }

      test("bind on Red carries prior warnings into Red") {
        trafficLight {
          warn("w1")
          Red<String, String>("err").bind()
          @Suppress("UNREACHABLE_CODE") 0
        } shouldBe Red("err", listOf("w1"))
      }

      test("warnings from bound Yellow combine with later warns") {
        trafficLight<Int, String, String> {
          val v = Yellow(1, nonEmptyListOf("upstream")).bind()
          warn("downstream")
          v + 1
        } shouldBe Yellow(2, nonEmptyListOf("upstream", "downstream"))
      }

      test("chaining multiple binds accumulates all warnings") {
        trafficLight<Int, String, String> {
          val a = Yellow(1, nonEmptyListOf("w1")).bind()
          val b = Yellow(2, nonEmptyListOf("w2")).bind()
          val c = Green(3).bind()
          a + b + c
        } shouldBe Yellow(6, nonEmptyListOf("w1", "w2"))
      }

      test("bind mid-chain stops at first Red, preserves prior warnings") {
        trafficLight {
          val a = Yellow(1, nonEmptyListOf("w1")).bind()
          Red<String, String>("err").bind()
          @Suppress("UNREACHABLE_CODE") Yellow(a + 1, nonEmptyListOf("w2")).bind()
          @Suppress("UNREACHABLE_CODE") 0
        } shouldBe Red("err", listOf("w1"))
      }
    }

    context("warn(Iterable)") {
      test("warn with a list of warnings adds all in order") {
        trafficLight<Int, String, String> {
          warn(listOf("a", "b", "c"))
          0
        } shouldBe Yellow(0, nonEmptyListOf("a", "b", "c"))
      }
    }
  })
