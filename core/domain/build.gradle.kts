plugins {
    alias(libs.plugins.cadejo.jvm.library)
}

// :core:domain is intentionally dependency-free (only Kotlin stdlib). JUnit is
// added for tests by the cadejo.jvm.library convention plugin. No Android, no
// coroutines in the pure rules — this is what keeps the game logic deterministic
// and runnable on any JVM, including a server that re-validates leaderboard scores.
