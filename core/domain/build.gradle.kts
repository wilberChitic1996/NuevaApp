plugins {
    alias(libs.plugins.cadejo.jvm.library)
}

// :core:domain stays Android-free — pure Kotlin so the game logic runs on any JVM
// (including a server that re-validates leaderboard scores). The only dependency is
// coroutines-core (a plain JVM library) so repository interfaces can expose Flow.
// JUnit is added for tests by the cadejo.jvm.library convention plugin.
dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
