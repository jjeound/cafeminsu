plugins {
    alias(libs.plugins.cafeminsu.android.library)
    alias(libs.plugins.cafeminsu.hilt)
}

android {
    namespace = "com.ssafy.cafeminsu.core.datastore"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.datastoreProto)

    api(libs.androidx.dataStore)
    implementation(libs.protobuf.kotlin.lite)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
