plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

/**
 * Касса для Android: телефоны и планшеты.
 *
 * Модуль только запускает приложение — весь код живёт в `shared`.
 * Kotlin здесь встроен в AGP 9, отдельного плагина Kotlin не нужно.
 */
android {
    namespace = "kz.mybrain.superkassa"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "kz.mybrain.superkassa"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = providers.gradleProperty("appVersion").getOrElse("1.0.0-dev")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.material3)
    implementation(libs.androidx.activity.compose)
}
