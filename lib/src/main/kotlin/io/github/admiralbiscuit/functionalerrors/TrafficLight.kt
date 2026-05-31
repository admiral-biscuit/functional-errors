package io.github.admiralbiscuit.functionalerrors

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

sealed interface TrafficLight<out A, out W, out E>

data class Green<out A>(val value: A) : TrafficLight<A, Nothing, Nothing> {
  override fun toString(): String = "TrafficLight.Green($value)"
}

data class Yellow<out A, out W>(val value: A, val warnings: NonEmptyList<W>) :
  TrafficLight<A, W, Nothing> {
  override fun toString(): String = "TrafficLight.Yellow($value, ${warnings.toList()})"
}

data class Red<out E, out W>(val value: E, val warnings: List<W> = emptyList()) :
  TrafficLight<Nothing, W, E> {
  override fun toString(): String = "TrafficLight.Red($value, $warnings)"
}

fun <A> A.green(): Green<A> = Green(this)

fun <A, W> A.yellow(firstWarning: W, vararg moreWarnings: W): Yellow<A, W> =
  Yellow(this, nonEmptyListOf(firstWarning, *moreWarnings))

fun <E, W> E.red(vararg warnings: W): Red<E, W> = Red(this, listOf(*warnings))

fun <A1, A2, W, E> TrafficLight<A1, W, E>.flatMap(
  f: (A1) -> TrafficLight<A2, W, E>
): TrafficLight<A2, W, E> =
  when (this) {
    is Green -> f(value)
    is Yellow ->
      when (val next = f(value)) {
        is Green -> Yellow(next.value, warnings)
        is Yellow -> Yellow(next.value, warnings + next.warnings)
        is Red -> Red(next.value, warnings + next.warnings)
      }
    is Red -> this
  }

fun <A1, A2, W, E> TrafficLight<A1, W, E>.map(f: (A1) -> A2) = flatMap { Green(f(it)) }

fun <A, W, E1, E2> TrafficLight<A, W, E1>.mapRed(f: (E1) -> E2) =
  when (this) {
    is Red -> Red(f(value), warnings)
    else -> this
  }

fun <A, W1, W2, E> TrafficLight<A, W1, E>.mapWarnings(f: (W1) -> W2): TrafficLight<A, W2, E> =
  when (this) {
    is Green -> this
    is Yellow -> Yellow(value, warnings.map(f))
    is Red -> Red(value, warnings.map(f))
  }
