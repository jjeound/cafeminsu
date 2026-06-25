import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val kakaoNativeAppKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY").orEmpty()
val baseUrl = localProperties.getProperty("BASE_URL").orEmpty().trim()
val kakaoPayEnabled = localProperties.getProperty("KAKAOPAY_ENABLED").orEmpty().trim()
    .ifBlank { "false" }

require(baseUrl.isBlank() || baseUrl.startsWith("https://")) {
    "BASE_URL must start with https:// when set."
}

require(kakaoPayEnabled == "true" || kakaoPayEnabled == "false") {
    "KAKAOPAY_ENABLED must be \"true\" or \"false\" when set."
}

fun String.toBuildConfigLiteral(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.cafeminsu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cafeminsu"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", kakaoNativeAppKey.toBuildConfigLiteral())
        buildConfigField("String", "BASE_URL", baseUrl.toBuildConfigLiteral())
        buildConfigField("boolean", "KAKAOPAY_ENABLED", kakaoPayEnabled)
        manifestPlaceholders["kakaoRedirectScheme"] = "kakao$kakaoNativeAppKey"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xskip-prerelease-check")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

// litertlm-android는 더 최신 Kotlin(stdlib/reflect 2.2.x)을 transitive로 끌어온다.
// Hilt 메타데이터 리더(≤2.1.0)·프로젝트 Kotlin(2.0.21)과 충돌하므로 프로젝트 버전으로 고정한다.
tasks.withType<KotlinCompile>().configureEach {
    exclude("**/com/cafeminsu/**")
}

configurations.all {
    resolutionStrategy {
        val kotlinVersion = libs.versions.kotlin.get()
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    }
}

// Opt-in 라이브 스모크 테스트(`com.cafeminsu.live.*`)용 시스템 프로퍼티를 forked 테스트 JVM 으로 전달한다.
// Gradle 은 기본적으로 CLI `-D` 를 테스트 JVM 에 넘기지 않으므로, 게이트 프로퍼티만 surgical 하게 전달한다.
// (미설정 시 라이브 테스트는 전부 skip — 기본 빌드 동작 불변.)
tasks.withType<Test>().configureEach {
    listOf("liveServer", "liveServer.baseUrl", "liveServer.token").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(projects.feature.auth)
    implementation(projects.feature.home)
    implementation(projects.feature.menu)
    implementation(projects.feature.store)
    implementation(projects.feature.my)
    implementation(projects.feature.notification)
    implementation(projects.feature.coupon)
    implementation(projects.feature.gift)
    implementation(projects.feature.history)
    implementation(projects.feature.payment)
    implementation(projects.feature.signup)
    implementation(projects.feature.stamp)
    implementation(projects.feature.voice)
    implementation(projects.feature.owner.home)
    implementation(projects.feature.owner.login)
    implementation(projects.feature.owner.menu)
    implementation(projects.feature.owner.orders)
    implementation(projects.feature.owner.sales)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kakao.user)
    implementation(libs.kakao.share)
    implementation(libs.kakao.friend)
    implementation(libs.kakao.talk)
    implementation(libs.kakao.map)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.coil.compose)
    implementation(libs.litertlm.android)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
