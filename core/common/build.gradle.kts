plugins {
    alias(libs.plugins.cafeminsu.jvm.library)
    alias(libs.plugins.cafeminsu.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}