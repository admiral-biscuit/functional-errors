// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.fold
import kotlin.experimental.ExperimentalTypeInference

class TrafficLightRaise<W, E>
@PublishedApi
internal constructor(
  @PublishedApi internal val accumulated: MutableList<W>,
  private val raise: Raise<E>,
) : Raise<E> by raise {

  fun warn(warning: W) {
    accumulated.add(warning)
  }

  fun warn(warnings: Iterable<W>) {
    accumulated.addAll(warnings)
  }

  fun <A> TrafficLight<A, W, E>.bind(): A =
    when (this) {
      is Green -> value
      is Yellow -> {
        warn(this.warnings)
        value
      }
      is Red -> raise(value)
    }
}

@OptIn(ExperimentalTypeInference::class)
inline fun <A, W, E> trafficLight(
  @BuilderInference block: TrafficLightRaise<W, E>.() -> A
): TrafficLight<A, W, E> {
  val accumulated = mutableListOf<W>()
  return fold(
    block = { block(TrafficLightRaise(accumulated, this)) },
    recover = { e: E -> Red(e, accumulated.toList()) },
    transform = { a: A ->
      if (accumulated.isEmpty()) Green(a)
      else Yellow(a, NonEmptyList(accumulated[0], accumulated.drop(1)))
    },
  )
}
