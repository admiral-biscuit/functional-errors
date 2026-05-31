// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TrafficLightTest :
  FunSpec({
    context("left identity") {
      test("Green flatMap returns f(a) when f returns Green") {
        Green(1).flatMap { Green(it * 2) } shouldBe Green(2)
      }

      test("Green flatMap returns f(a) when f returns Yellow") {
        Green(1).flatMap { Yellow(it * 2, nonEmptyListOf("w")) } shouldBe
          Yellow(2, nonEmptyListOf("w"))
      }

      test("Green flatMap returns f(a) when f returns Red") {
        Green(1).flatMap { Red("err") } shouldBe Red("err", emptyList())
      }
    }

    context("right identity") {
      test("Green flatMap Green(it) is identity") {
        Green(42).flatMap { Green(it) } shouldBe Green(42)
      }

      test("Yellow flatMap Green(it) is identity") {
        val m = Yellow(42, nonEmptyListOf("w1"))
        m.flatMap { Green(it) } shouldBe m
      }

      @Suppress("UNREACHABLE_CODE")
      test("Red flatMap Green(it) is identity") {
        val m = Red("err", listOf("w1"))
        m.flatMap { Green(it) } shouldBe m
      }
    }

    context("associativity") {
      val f: (Int) -> TrafficLight<Int, String, String> = {
        Yellow(it + 1, nonEmptyListOf("f-warn"))
      }
      val g: (Int) -> TrafficLight<Int, String, String> = {
        Yellow(it * 10, nonEmptyListOf("g-warn"))
      }

      test("Green associativity") {
        val m = Green(0)
        m.flatMap(f).flatMap(g) shouldBe m.flatMap { f(it).flatMap(g) }
      }

      test("Yellow associativity") {
        val m = Yellow(0, nonEmptyListOf("init"))
        m.flatMap(f).flatMap(g) shouldBe m.flatMap { f(it).flatMap(g) }
      }

      @Suppress("UNREACHABLE_CODE")
      test("Red associativity") {
        val m = Red("err", listOf("init"))
        m.flatMap(f).flatMap(g) shouldBe m.flatMap { f(it).flatMap(g) }
      }
    }

    context("warning accumulation") {
      test("Yellow + Green preserves upstream warnings") {
        Yellow(1, nonEmptyListOf("w1")).flatMap { Green(it + 1) } shouldBe
          Yellow(2, nonEmptyListOf("w1"))
      }

      test("Yellow + Yellow merges warnings in order") {
        Yellow(1, nonEmptyListOf("w1")).flatMap { Yellow(it + 1, nonEmptyListOf("w2")) } shouldBe
          Yellow(2, nonEmptyListOf("w1", "w2"))
      }

      test("Yellow + Red carries upstream warnings into Red") {
        Yellow(1, nonEmptyListOf("w1")).flatMap { Red("err", listOf("w2")) } shouldBe
          Red("err", listOf("w1", "w2"))
      }

      test("Green + Yellow has only downstream warnings") {
        Green(1).flatMap { Yellow(it + 1, nonEmptyListOf("w1")) } shouldBe
          Yellow(2, nonEmptyListOf("w1"))
      }

      test("three-step chain accumulates all warnings in order") {
        val result =
          Yellow(0, nonEmptyListOf("a"))
            .flatMap { Yellow(it + 1, nonEmptyListOf("b")) }
            .flatMap { Yellow(it + 1, nonEmptyListOf("c")) }
        result shouldBe Yellow(2, nonEmptyListOf("a", "b", "c"))
      }
    }

    context("Red short-circuits") {
      @Suppress("UNREACHABLE_CODE")
      test("Red.flatMap ignores f") {
        var called = false
        Red<String, Nothing>("err").flatMap {
          called = true
          Green(it)
        }
        called shouldBe false
      }

      @Suppress("UNREACHABLE_CODE")
      test("Red after Yellow still short-circuits subsequent steps") {
        val red: TrafficLight<Int, String, String> =
          Yellow(1, nonEmptyListOf("w1")).flatMap { Red("err") }

        red.flatMap { Yellow(it + 1, nonEmptyListOf("w2")) } shouldBe Red("err", listOf("w1"))
      }
    }

    context("map") {
      test("map over Green") { Green(2).map { it * 3 } shouldBe Green(6) }

      test("map over Yellow preserves warnings") {
        Yellow(2, nonEmptyListOf("w")).map { it * 3 } shouldBe Yellow(6, nonEmptyListOf("w"))
      }

      test("map over Red is identity") {
        val red: TrafficLight<Int, String, String> = Red("err", listOf("w"))
        red.map { it * 3 } shouldBe red
      }
    }

    context("mapRed") {
      test("mapRed transforms Red error value") {
        Red("oops", listOf("w")).mapRed { it.length } shouldBe Red(4, listOf("w"))
      }

      test("mapRed on Green is identity") {
        val green: TrafficLight<Int, String, String> = Green(1)
        green.mapRed { "x" } shouldBe green
      }

      test("mapRed on Yellow is identity") {
        val yellow: TrafficLight<Int, String, String> = Yellow(1, nonEmptyListOf("w"))
        yellow.mapRed { "x" } shouldBe yellow
      }
    }

    context("mapWarnings") {
      test("mapWarnings on Green is identity") {
        val green: TrafficLight<Int, String, String> = Green(1)
        green.mapWarnings { it.uppercase() } shouldBe green
      }

      test("mapWarnings transforms Yellow warnings") {
        Yellow(1, nonEmptyListOf("warn")).mapWarnings { it.uppercase() } shouldBe
          Yellow(1, nonEmptyListOf("WARN"))
      }

      test("mapWarnings transforms Red warnings") {
        Red("err", listOf("warn")).mapWarnings { it.uppercase() } shouldBe
          Red("err", listOf("WARN"))
      }
    }

    context("toString") {
      test("Green") { Green(1).toString() shouldBe "TrafficLight.Green(1)" }

      test("Yellow") {
        Yellow(1, nonEmptyListOf("w")).toString() shouldBe "TrafficLight.Yellow(1, [w])"
      }

      test("Red") { Red("err", listOf("w")).toString() shouldBe "TrafficLight.Red(err, [w])" }
    }
  })
