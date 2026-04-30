# functional-errors

A small Kotlin library for typesafe, non-exceptional error handling built on top of
[Arrow](https://arrow-kt.io)'s `Either`.

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
    implementation("io.github.admiral-biscuit:functional-errors:0.0.1")
}
```

## Usage

### Define your failures

Implement `Failure` on each error type in your domain:

```kotlin
data class DatabaseFailure(
    override val message: String,
    override val cause: Cause? = null,
) : Failure

data class ServiceFailure(
    override val message: String,
    override val cause: Cause? = null,
) : Failure
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
) : Failure

fun getUserProfile(id: UserId): Either<ServiceFailure, UserProfile> =
    findUser(id)
        .map { user -> UserProfile(user) }
        .mapLeft { ServiceFailure("Could not load user profile", "getUserProfile", it.toCause()) }
```

### Bridge exception-based code

Use `catchAndCauseFailure` to catch any `Throwable` thrown by a block and convert it into a typed
failure:

```kotlin
suspend fun findUser(id: UserId): Either<DatabaseFailure, User> =
    catchAndCauseFailure("Database query failed", ::DatabaseFailure) {
        database.query("SELECT * FROM users WHERE id = ?", id)
    }
```

### Inspect the causal chain

`toPrettyString` renders the full chain of causes, similar to how an exception prints its stack
trace:

```kotlin
val failure: ServiceFailure = TODO()
println(failure.toPrettyString())
```

```
ServiceFailure: Could not load user profile
Caused by: DatabaseFailure: Database query failed
Caused by: java.sql.SQLException: Connection reset
	at com.example.Database.query(Database.kt:42)
	at com.example.UserRepository.findUser(UserRepository.kt:17)
	...
```

For programmatic inspection, `causalChain()` returns the list of `Cause` entries and `rootCause()`
returns the last one.

## Requirements

- Kotlin 2.x
- Arrow 2.x
- Java 17+
