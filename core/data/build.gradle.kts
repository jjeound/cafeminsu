plugins {
    alias(libs.plugins.cafeminsu.android.library)
    alias(libs.plugins.cafeminsu.hilt)
}

android {
    namespace = "com.ssafy.cafeminsu.core.data"
}

dependencies {
    // core modules
    api(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.common)
    implementation(libs.sandwich)
    implementation(libs.kakao.user)

    // kotlinx
    api(libs.kotlinx.immutable.collection)

    // coroutines
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)


    // unit test
    testImplementation(libs.junit)
    testImplementation(libs.protobuf.kotlin.lite)
}
