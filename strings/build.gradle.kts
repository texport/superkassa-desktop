plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

/**
 * Тексты кассы на трёх языках — казахском, русском и английском.
 *
 * Модуль знает только слова: ни Compose, ни ядра, ни домена приложения.
 * Наружу виден пакет `api` — язык, формы текстов и одна точка входа
 * `textsOf`; таблицы языков в `impl` закрыты модификатором `internal`,
 * и снаружи модуля их не вызвать — это проверяет компилятор.
 *
 * Цели те же, что у модулей интеграций: код целиком общий, и iOS
 * получает те же тексты без единой платформенной строки.
 */
kotlin {
    jvmToolchain(21)

    jvm()
    android {
        namespace = "kz.mybrain.superkassa.strings"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
