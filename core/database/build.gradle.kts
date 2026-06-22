plugins {
    alias(libs.plugins.cafeminsu.android.library)
    alias(libs.plugins.cafeminsu.android.room)
    alias(libs.plugins.cafeminsu.android.hilt)
}

android {
    namespace = "com.ssafy.cafeminsu.core.database"
}

dependencies {
    api(projects.core.model)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}