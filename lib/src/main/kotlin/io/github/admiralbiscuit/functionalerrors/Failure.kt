// SPDX-License-Identifier: MIT-0
package io.github.admiralbiscuit.functionalerrors

import arrow.core.Either

// region Failure
private const val MAX_CHAIN_LENGTH = 999

private fun captureCreationSite(): StackTraceElement? =
  Throwable().stackTrace.firstOrNull { frame ->
    frame.methodName != "<init>" &&
      frame.fileName != "Failure.kt" &&
      !frame.className.startsWith("java.") &&
      !frame.className.startsWith("kotlin.")
  }

/**
 * Base class for typesafe, non-exceptional errors intended to be used with Arrow's [Either].
 *
 * Extend this class on each error type in your domain. A [Failure] carries a [message] describing
 * what went wrong and an optional [cause] linking it to the underlying [Failure] or [Throwable]
 * that triggered it, forming a causal chain analogous to an exception stack trace.
 *
 * Subclasses can narrow the type of [cause] by overriding it covariantly, so that the wrapped
 * [Failure] keeps its concrete type:
 * ```
 * abstract class AppFailure(
 *   override val message: String,
 *   override val cause: Cause<AppFailure>? = null,
 * ) : Failure(message, cause) {
 *   abstract val code: Int
 * }
 * ```
 *
 * A narrowed [cause] still accepts a [ThrowableCause], and can be narrowed further in subclasses
 * but never widened again.
 *
 * The [createdAt] property captures the call site where the failure was instantiated, analogous to
 * the top frame of an exception stack trace.
 */
abstract class Failure(
  open val message: String,
  open val cause: Cause<*>? = null,
  val createdAt: StackTraceElement? = captureCreationSite(),
) {

  /**
   * Returns all [Cause] entries in the causal chain, starting from [cause] and following each
   * [FailureCause] until the chain ends or [max] is reached.
   *
   * By default, the chain also stops at the first [ThrowableCause], because a [Throwable] already
   * carries its own stack trace. Pass `false` to [stopAtFirstThrowable] to continue through the
   * [Throwable]'s own cause chain as well.
   *
   * The [max] parameter guards against infinite loops in case of a self-referencing [Failure].
   */
  fun causalChain(
    stopAtFirstThrowable: Boolean = true,
    max: Int = MAX_CHAIN_LENGTH,
  ): List<Cause<*>> =
    generateSequence(this.cause) { cause ->
        when (cause) {
          is FailureCause<*> -> cause.failure.cause
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
   * Returns the last element of [causalChain], or `null` if this [Failure] has no cause.
   *
   * When the chain reaches a [ThrowableCause], the result depends on [stopAtFirstThrowable]:
   * - `true` (default): that [ThrowableCause] is returned.
   * - `false`: the root [ThrowableCause] at the end of the [Throwable]'s own cause chain is
   *   returned.
   */
  fun rootCause(stopAtFirstThrowable: Boolean = true): Cause<*>? =
    causalChain(stopAtFirstThrowable).lastOrNull()

  /**
   * Returns a string representation of this [Failure], mirroring the format of a Java exception:
   * ```
   * ClassName: message
   *     at ClassName.method(FileName.kt:line)
   * ```
   *
   * The `at` line is omitted when [createdAt] is `null`. The location uses
   * [StackTraceElement.toString], so IDEs render it as a clickable link.
   *
   * Override this function to customize how a [Failure] subclass is rendered. [toPrettyString] uses
   * it by default for every [Failure] in the causal chain.
   */
  open fun toSimpleString(): String {
    val location = createdAt?.let { "\n\tat $it" } ?: ""
    return "${javaClass.simpleName}: $message$location"
  }

  /**
   * Returns a multi-line string representation of this [Failure] and its full [causalChain].
   *
   * By default, each entry is rendered with [toSimpleString] for [FailureCause] entries and
   * [Throwable.stackTraceToString] for [ThrowableCause] entries, joined by `"\nCaused by: "`. All
   * three formatting steps can be overridden via [failureToString], [throwableToString], and
   * [joinStrings].
   *
   * To change how a [Failure] subclass is always rendered, override [toSimpleString] instead. For a
   * one-off format, pass a [failureToString] that checks the concrete type. Since the causal chain
   * can mix different [Failure] types, [failureToString] always receives a plain [Failure].
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
            is FailureCause<*> -> failureToString(cause.failure)
            is ThrowableCause -> throwableToString(cause.throwable)
          }
        }

    return joinStrings(strings)
  }
}

// endregion

// region Cause
/** The cause of a [Failure]: either another [Failure] or a [Throwable]. */
sealed interface Cause<out F : Failure>

// Remark:
// This is deliberately not a value class.
// With a generic value class,
// the Kotlin compiler can access a property of the unboxed failure on the erased type Failure
// without a cast,
// which fails at runtime with a NoSuchFieldError.

/** Wraps a [Failure] as the cause of another [Failure]. */
data class FailureCause<out F : Failure>(val failure: F) : Cause<F>

/** Wraps a [Throwable] as the cause of a [Failure], bridging exception-based code. */
@JvmInline value class ThrowableCause(val throwable: Throwable) : Cause<Nothing>

// endregion

// region extension functions
/** Wraps this [Failure] as a [FailureCause]. Useful when constructing a new [Failure] manually. */
fun <F : Failure> F.toCause(): FailureCause<F> = FailureCause(this)

/**
 * Wraps this [Throwable] as a [ThrowableCause]. Useful when constructing a new [Failure] manually.
 */
fun Throwable.toCause(): ThrowableCause = ThrowableCause(this)

/**
 * Creates a new [F2] caused by this [Failure], using [transformation] to construct it.
 *
 * Constructor references work for simple failures: `failure.causeFailure("message", ::MyFailure)`.
 */
fun <F1 : Failure, F2 : Failure> F1.causeFailure(
  message: String,
  transformation: (String, FailureCause<F1>) -> F2,
): F2 = transformation(message, FailureCause(this))

/**
 * Creates a new [F] caused by this [Throwable], using [transformation] to construct it.
 *
 * Constructor references work for simple failures: `throwable.causeFailure("message",
 * ::MyFailure)`.
 */
fun <F : Failure> Throwable.causeFailure(
  message: String,
  transformation: (String, ThrowableCause) -> F,
): F = transformation(message, ThrowableCause(this))

/**
 * Maps the [Either.Left] to a new [F2] caused by the original failure. [Either.Right] values pass
 * through unchanged.
 *
 * Constructor references work for simple failures: `either.causeFailure("message", ::MyFailure)`.
 */
fun <F1 : Failure, F2 : Failure, R> Either<F1, R>.causeFailure(
  message: String,
  transformation: (String, FailureCause<F1>) -> F2,
): Either<F2, R> = mapLeft { failure -> failure.causeFailure(message, transformation) }

/**
 * Runs [f] and returns its result as [Either.Right]. If [f] throws, the [Throwable] is wrapped in a
 * new [F] built by [transformation] and returned as [Either.Left].
 */
fun <F : Failure, R> catchAndCauseFailure(
  message: String,
  transformation: (String, ThrowableCause) -> F,
  f: () -> R,
): Either<F, R> =
  Either.catch { f() }.mapLeft { throwable -> throwable.causeFailure(message, transformation) }

/** Suspending variant of [catchAndCauseFailure] for use with suspend functions. */
suspend fun <F : Failure, R> suspendCatchAndCauseFailure(
  message: String,
  transformation: (String, ThrowableCause) -> F,
  f: suspend () -> R,
): Either<F, R> =
  Either.catch { f() }.mapLeft { throwable -> throwable.causeFailure(message, transformation) }

// endregion
