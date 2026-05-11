# functional-errors

A small Kotlin library for typesafe, non-exceptional error handling built on top of
[Arrow](https://arrow-kt.io)'s `Either`.

Originally developed at [tech@spree](https://spree.de/). See also my more detailed [blog post](https://blog.spree.de/2026/05/11/failure-kt-a-tiny-stacktrace-library-on-top-of-arrows-either/).

## The problem

Arrow's `Either` makes it possible to pass errors between service layers as typed values instead of
exceptions. What it does not provide out of the box is a way to retain context as an error travels
upward through those layers — the equivalent of an exception's stack trace.

Consider a `UserRepository` that returns `Either<DatabaseFailure, User>`, called by a
`UserService` that returns `Either<UserServiceFailure, User>`. When the repository fails, the
service needs to wrap that failure in its own error type without losing the original context.

This library provides the building blocks to do that cleanly.

## Installation

```kotlin
  dependencies {
    implementation("io.github.admiral-biscuit:functional-errors:0.2.0")
  }
```

For Kotest assertion support, also add the companion library to your test dependencies:

```kotlin
  dependencies {
    testImplementation("io.github.admiral-biscuit:functional-errors-kotest:0.2.0")
  }
```

## Example

A self-contained, runnable example covering sealed failure hierarchies, `causeFailure`,
`catchAndCauseFailure`, and Arrow's `either` DSL is in
[`examples/src/main/kotlin/.../examples/Main.kt`](examples/src/main/kotlin/io/github/admiralbiscuit/functionalerrors/examples/Main.kt).

```
  ./gradlew :examples:run
```

## Usage

### Define your failures

Extend `Failure` for each error type. Simple failures are data classes:

```kotlin
  data class DatabaseFailure(
    override val message: String,
    override val cause: Cause? = null,
  ) : Failure(message, cause)
```

For a service layer with multiple error cases, a sealed class works well. Notice that `Unexpected`
requires a non-nullable `cause` — making it structurally impossible to wrap an error without
preserving its context:

```kotlin
  sealed class UserServiceFailure(
    override val message: String,
    override val cause: Cause? = null,
  ) : Failure(message, cause) {
    data class Unexpected(override val message: String, override val cause: Cause) :
      UserServiceFailure(message, cause)

    data class UserNotFound(val id: Int) : UserServiceFailure("User $id not found")
  }
```

### Bridge exception-based code

Use `catchAndCauseFailure` to convert a throwing block into an `Either`:

```kotlin
  fun getUserByIdEither(id: Int): Either<DatabaseFailure, User?> =
    catchAndCauseFailure("connection to database not possible", ::DatabaseFailure) {
      getUserByIdThrowing(id)
    }
```

Use `suspendCatchAndCauseFailure` for blocks that call suspend functions.

### Chain failures across layers

Use `causeFailure` to wrap an `Either` from a lower layer, preserving the original failure as the
cause. Combined with Arrow's `either` DSL:

```kotlin
  fun getUserById(id: Int): Either<UserServiceFailure, User> = either {
    val userOrNull =
      getUserByIdEither(id)
        .causeFailure("failed to get user from repository", ::Unexpected)
        .bind()

    ensureNotNull(userOrNull) { UserServiceFailure.UserNotFound(id) }
  }
```

### Inspect the causal chain

`toPrettyString` renders the full chain of causes, mirroring the format of a Java exception with
its `Caused by:` chain. Each `Failure` entry includes a clickable link to the line where it was
instantiated:

```kotlin
  getUserById(13).onLeft { println(it.toPrettyString()) }
```

```
  Unexpected: failed to get user from repository
    at UserService.getUserById(Main.kt:42)
  Caused by: DatabaseFailure: connection to database not possible
    at UserRepository.getUserByIdEither(Main.kt:34)
  Caused by: java.lang.IllegalStateException: connection to database not possible
    at UserRepository.getUserByIdThrowing(Main.kt:29)
    ...
```

For programmatic inspection, `causalChain()` returns the list of `Cause` entries and `rootCause()`
returns the last one.

## Testing

The `functional-errors-kotest` library provides Kotest matchers for asserting on failures. The
matchers return the receiver (or the extracted cause), so they can be chained all the way through
the causal chain:

```kotlin
  UserService.getUserById(13)
    .shouldBeLeft()
    .shouldHaveMessage("failed to get user from repository")
    .shouldHaveFailureCause()
    .shouldHaveMessage("connection to database not possible")
    .shouldHaveThrowableCause()
    .message shouldBe "connection to database not possible"
```

## Requirements

- Kotlin 2.x
- Arrow 2.x
- Java 17+

`functional-errors-kotest` additionally requires Kotest 5.x.
