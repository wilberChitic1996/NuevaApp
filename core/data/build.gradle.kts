plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.hilt)
}

android {
    namespace = "gt.guardian.cadejo.core.data"

    // Room exports its schema JSON so migrations can be reviewed and tested.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

// :core:data implements the interfaces declared by :core:domain and owns all the
// framework-touching concerns: Room persistence, the Android Keystore HMAC signer,
// DataStore settings, and (later phases) networking, ads and billing.
dependencies {
    implementation(project(":core:domain"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // Monetization SDKs
    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)
    implementation(libs.user.messaging.platform)

    implementation(libs.timber)

    testImplementation(libs.kotlinx.coroutines.test)
}
