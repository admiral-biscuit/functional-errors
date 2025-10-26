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

  fun causes(max: Int = 999): List<Cause> =
    generateSequence(this.cause) { cause ->
        when (cause) {
          is FailureCause -> cause.failure.cause
          is ThrowableCause -> cause.throwable.cause?.let { throwable -> ThrowableCause(throwable) }
        }
      }
      .take(max)
      .toList()

  fun rootCause(): Cause? = causes().lastOrNull()

  fun print(max: Int = 999): String {
    val simpleStrings: List<String> =
      listOf(toSimpleString()) +
        causes(max).map { cause ->
          when (cause) {
            is FailureCause -> cause.failure.toSimpleString()
            is ThrowableCause -> cause.throwable.toSimpleString()
          }
        }

    return simpleStrings.joinToString("\ncaused by ")
  }
}

// Cause

/** A [Failure] can either be caused by another [Failure] or by a [Throwable]. */
sealed interface Cause

data class FailureCause(val failure: Failure) : Cause

data class ThrowableCause(val throwable: Throwable) : Cause

// extensions

fun Failure.toSimpleString(): String = "${javaClass.simpleName}: $message"

fun Throwable.toSimpleString(): String = "${javaClass.simpleName}: $message"

fun Failure.causeFailure(message: String, transformation: (String, Cause) -> Failure): Failure =
  transformation(message, FailureCause(this))

fun Throwable.causeFailure(message: String, transformation: (String, Cause) -> Failure): Failure =
  transformation(message, ThrowableCause(this))

fun <R> Either<Failure, R>.causeFailure(
  message: String,
  transformation: (String, Cause) -> Failure,
): Either<Failure, R> = mapLeft { failure -> failure.causeFailure(message, transformation) }

suspend fun <R> catchAndCauseFailure(
  message: String,
  transformation: (String, Cause) -> Failure,
  f: suspend () -> R,
): Either<Failure, R> =
  Either.catch { f() }.mapLeft { throwable -> throwable.causeFailure(message, transformation) }
