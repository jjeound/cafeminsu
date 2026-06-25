plugins {
    alias(libs.plugins.cafeminsu.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ssafy.cafeminsu.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
}
