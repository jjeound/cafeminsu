import java.util.Properties

val baseUrl = Properties().run {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
    getProperty("BASE_URL").orEmpty().trim()
}

fun String.toBuildConfigLiteral(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

plugins {
    alias(libs.plugins.cafeminsu.android.library)
    alias(libs.plugins.cafeminsu.android.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.cafeminsu.core.network"

    defaultConfig {
        buildConfigField("String", "BASE_URL", baseUrl.toBuildConfigLiteral())
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.retrofit.bom))
    implementation(platform(libs.okhttp.bom))
    implementation(libs.sandwich)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // json parsing

    // todo: ktor 의존성 추가
}
