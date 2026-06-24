plugins {
    alias(libs.plugins.cafeminsu.android.library)
    alias(libs.plugins.cafeminsu.android.library.compose)
    alias(libs.plugins.cafeminsu.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.cafeminsu.core.navigation"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Navigation3
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    // json parsing
    implementation(libs.kotlinx.serialization.json)
}