plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.polysius.applocker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.polysius.applocker"
        // 34: Health Connect is part of the Android framework from API 34 (spec section 9).
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-phase0"
    }

    buildTypes {
        debug {
            // Lets createDebugUnitTestCoverageReport measure the rules engine (spec section 13.1).
            enableUnitTestCoverage = true
        }
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    // Room writes each schema version here so later migrations can be tested against it.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.health.connect.client)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
