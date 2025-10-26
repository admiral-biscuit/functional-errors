package io.github.admiralbiscuit.functionalerrors

import arrow.core.Either

/**
 * The [Failure] class is meant to be used together with Arrow's [Either] in order to model
 * non-exceptional errors.
 *
 * A failure has to implement a [message] field (which may be null) and a [cause] field (which can
 * be a [FailureCause] or a [ThrowableCause] or null).
 *
 * The [Failure] interface is equipped with some convenience methods that can be used to emulate a
 * stack trace.
 */
interface Failure {
  val message: String?
  val cause: Cause?

  /**
   * Returns the list of causes ([Cause] instances) until the [cause] of a [Failure] is null. The
   * causal chain also stops at [ThrowableCause] because a [Throwable] has its own stack trace.
   *
   * The [max] parameter has the main purpose of making the function call stack-safe in case of a
   * self reference.
   */
  fun causalChain(max: Int = 999): List<Cause> =
    generateSequence(this.cause) { cause ->
        when (cause) {
          is FailureCause -> cause.failure.cause
          is ThrowableCause -> null
        }
      }
      .take(max)
      .toList()

  /**
   * Returns the last element of the [causalChain] (or null). Note that if the [Failure] is caused
   * by a [ThrowableCause], then this method will return that [ThrowableCause] – which has its own
   * stack trace – and *not* the deepest [Throwable] down the chain.
   */
  fun rootCause(): Cause? = causalChain().lastOrNull()

  fun toSimpleString(): String = "${javaClass.simpleName}: $message"

  fun print(max: Int = 999): String {
    val simpleStrings: List<String> =
      listOf(toSimpleString()) +
        causalChain(max).map { cause ->
          when (cause) {
            is FailureCause -> cause.failure.toSimpleString()
            is ThrowableCause -> cause.throwable.stackTraceToString()
          }
        }

    return simpleStrings.joinToString("\nCaused by: ")
  }
}

// Cause

/** A [Failure] can either be caused by another [Failure] or by a [Throwable]. */
sealed interface Cause

data class FailureCause(val failure: Failure) : Cause

data class ThrowableCause(val throwable: Throwable) : Cause

// extensions

fun <F1 : Failure, F2 : Failure> F1.causeFailure(
  message: String,
  transformation: (String, Cause) -> F2,
): F2 = transformation(message, FailureCause(this))

fun <F : Failure> Throwable.causeFailure(message: String, transformation: (String, Cause) -> F): F =
  transformation(message, ThrowableCause(this))

fun <F1 : Failure, F2 : Failure, R> Either<F1, R>.causeFailure(
  message: String,
  transformation: (String, Cause) -> F2,
): Either<F2, R> = mapLeft { failure -> failure.causeFailure(message, transformation) }

suspend fun <F : Failure, R> catchAndCauseFailure(
  message: String,
  transformation: (String, Cause) -> F,
  f: suspend () -> R,
): Either<F, R> =
  Either.catch { f() }.mapLeft { throwable -> throwable.causeFailure(message, transformation) }
