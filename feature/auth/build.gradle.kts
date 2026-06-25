plugins {
    id("cafeminsu.android.feature")
    id("cafeminsu.android.hilt")
}

android {
    namespace = "com.ssafy.cafeminsu.compose.feature.auth"
}

dependencies {
    implementation(libs.androidx.hilt.navigation.compose)
}
