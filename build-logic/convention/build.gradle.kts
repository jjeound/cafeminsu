plugins {
    `kotlin-dsl`
}

group = "com.ssafy.cafeminsu.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = libs.plugins.cafeminsu.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }

        register("androidApplication") {
            id = libs.plugins.cafeminsu.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }

        register("androidLibraryCompose") {
            id = libs.plugins.cafeminsu.android.library.compose.get().pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }

        register("androidLibrary") {
            id = libs.plugins.cafeminsu.android.library.asProvider().get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }

        register("androidFeature") {
            id = libs.plugins.cafeminsu.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }

        register("androidRoom") {
            id = libs.plugins.cafeminsu.android.room.get().pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }

        register("androidHilt") {
            id = libs.plugins.cafeminsu.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }

        register("jvmLibrary") {
            id = "cafeminsu.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}