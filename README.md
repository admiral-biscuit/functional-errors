# functional-errors

A small Kotlin library for typesafe, non-exceptional error handling built on top of
[Arrow](https://arrow-kt.io)'s `Either`.

Originally developed at [tech@spree](https://spree.de/).

## The problem

Arrow's `Either` makes it possible to pass errors between service layers as typed values instead of
exceptions. What it does not provide out of the box is a way to retain context as an error travels
upward through those layers — the equivalent of an exception's stack trace.

Consider a `UserRepository` that returns `Either<DatabaseFailure, User>`, called by a
`UserService` that returns `Either<ServiceFailure, UserProfile>`. When the repository fails, the
service needs to wrap that failure in its own error type without losing the original context.

This library provides the building blocks to do that cleanly.

## Installation

```kotlin
dependencies {
    implementation("io.github.admiral-biscuit:functional-errors:0.1.1")
}
```

For Kotest assertion support, also add the companion library to your test dependencies:

```kotlin
dependencies {
    testImplementation("io.github.admiral-biscuit:functional-errors-kotest:0.1.1")
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

Extend `Failure` for each error type in your domain:

```kotlin
data class DatabaseFailure(
    override val message: String,
    override val cause: Cause? = null,
) : Failure(message, cause)

data class ServiceFailure(
    override val message: String,
    override val cause: Cause? = null,
) : Failure(message, cause)
```

### Chain failures across layers

Use `causeFailure` to wrap a failure from a lower layer into the type expected by the layer above,
preserving the original as the cause:

```kotlin
fun findUser(id: UserId): Either<DatabaseFailure, User> = TODO()

fun getUserProfile(id: UserId): Either<ServiceFailure, UserProfile> =
    findUser(id)
        .map { user -> UserProfile(user) }
        .causeFailure("Could not load user profile", ::ServiceFailure)
```

For failures with extra fields, use `toCause()` directly:

```kotlin
data class ServiceFailure(
    override val message: String,
    val operation: String,
    override val cause: Cause? = null,
) : Failure(message, cause)

fun getUserProfile(id: UserId): Either<ServiceFailure, UserProfile> =
    findUser(id)
        .map { user -> UserProfile(user) }
        .mapLeft { ServiceFailure("Could not load user profile", "getUserProfile", it.toCause()) }
```

### Bridge exception-based code

Use `catchAndCauseFailure` or `suspendCatchAndCauseFailure` to catch any `Throwable` thrown by a block and convert it into a typed
failure:

```kotlin
fun findUser(id: UserId): Either<DatabaseFailure, User> =
    catchAndCauseFailure("Database query failed", ::DatabaseFailure) {
        database.query("SELECT * FROM users WHERE id = ?", id)
    }
```

### Inspect the causal chain

`toPrettyString` renders the full chain of causes, mirroring the format of a Java exception with
its `Caused by:` chain. Each `Failure` entry includes a clickable link to the line where it was
instantiated:

```kotlin
val failure: ServiceFailure = TODO()
println(failure.toPrettyString())
```

```
ServiceFailure: Could not load user profile
	at com.example.UserService.getUserProfile(UserService.kt:22)
Caused by: DatabaseFailure: Database query failed
	at com.example.UserRepository.findUser(UserRepository.kt:17)
Caused by: java.sql.SQLException: Connection reset
	at com.example.Database.query(Database.kt:42)
	...
```

For programmatic inspection, `causalChain()` returns the list of `Cause` entries and `rootCause()`
returns the last one.

## Testing

The `functional-errors-kotest` library provides Kotest matchers for asserting on failures. The
matchers return the receiver (or the extracted cause), so they can be chained:

```kotlin
val failure: ServiceFailure = TODO()

// dot-chaining style
failure
    .shouldHaveMessage("Could not load user profile")
    .shouldHaveFailureCause(databaseFailure)

// infix style
failure shouldHaveMessage "Could not load user profile"
failure shouldHaveFailureCause databaseFailure

// extract the cause for further assertions
failure
    .shouldHaveThrowableCause()
    .message shouldBe "Connection reset"

// inspect the instantiation site
failure.createdAt?.fileName  // e.g. "UserService.kt"
failure.createdAt?.lineNumber
```

## Requirements

- Kotlin 2.x
- Arrow 2.x
- Java 17+

`functional-errors-kotest` additionally requires Kotest 5.x.
