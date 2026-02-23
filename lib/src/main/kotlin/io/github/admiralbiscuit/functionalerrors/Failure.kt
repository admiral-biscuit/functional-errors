package io.github.admiralbiscuit.functionalerrors

import arrow.core.Either

private const val MAX_CHAIN_LENGTH = 999

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
   * Returns the list of causes ([Cause] instances) until the [cause] of a [Failure] is null. By
   * default, the causal chain also stops at the first [ThrowableCause] because a [Throwable] has
   * its own stack trace. If desired, this behavior can be changed by passing `false` to
   * [stopAtFirstThrowable] – the chain will then continue to the root [Throwable].
   *
   * The [max] parameter has the main purpose of making the function call stack-safe in case of a
   * self reference.
   */
  fun causalChain(stopAtFirstThrowable: Boolean = true, max: Int = MAX_CHAIN_LENGTH): List<Cause> =
    generateSequence(this.cause) { cause ->
        when (cause) {
          is FailureCause -> cause.failure.cause
          is ThrowableCause ->
            if (stopAtFirstThrowable) {
              null
            } else {
              cause.throwable.cause?.let { ThrowableCause(it) }
            }
        }
      }
      .take(max)
      .toList()

  /**
   * Returns the last element of the [causalChain] (or null). If the given [Failure] is at some
   * point caused by a [ThrowableCause], then the return value of this method depends on what is
   * passed to [stopAtFirstThrowable]:
   * - If `true` (default), that [ThrowableCause] will be returned.
   * - If `false`, the root throwable will be returned as a [ThrowableCause].
   */
  fun rootCause(stopAtFirstThrowable: Boolean = true): Cause? =
    causalChain(stopAtFirstThrowable).lastOrNull()

  fun toSimpleString(): String = "${javaClass.simpleName}: $message"

  /**
   * Returns a string representation of the whole [causalChain], including the [Failure] at the top.
   * The formatting can be customized by optionally providing functions to the arguments
   * [failureToString], [throwableToString], and [joinStrings].
   */
  fun toPrettyString(
    failureToString: (Failure) -> String = { failure -> failure.toSimpleString() },
    throwableToString: (Throwable) -> String = { throwable -> throwable.stackTraceToString() },
    joinStrings: (List<String>) -> String = { strings -> strings.joinToString("\nCaused by: ") },
    stopAtFirstThrowable: Boolean = true,
    max: Int = MAX_CHAIN_LENGTH,
  ): String {
    val strings: List<String> =
      listOf(failureToString(this)) +
        causalChain(stopAtFirstThrowable, max).map { cause ->
          when (cause) {
            is FailureCause -> failureToString(cause.failure)
            is ThrowableCause -> throwableToString(cause.throwable)
          }
        }

    return joinStrings(strings)
  }
}

// Cause

/** A [Failure] can either be caused by another [Failure] or by a [Throwable]. */
sealed interface Cause

@JvmInline value class FailureCause(val failure: Failure) : Cause

@JvmInline value class ThrowableCause(val throwable: Throwable) : Cause

// extension functions

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
