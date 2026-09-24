plugins {
    `kotlin-dsl`
}

/**
 * Общая настройка модулей Kotlin Multiplatform кассы.
 *
 * Модули библиотек — тексты, домен, данные, оснастка проверок, интеграции —
 * объявляли одни и те же цели, одну и ту же Android-цель из каталога версий
 * и одно и то же пространство имён по своему пути. Модули экранов — общее
 * экранов и области — сверх того одинаково подключают Compose и проверки.
 * Здесь это записано один раз; модуль называет только свои зависимости.
 *
 * Плагины Kotlin и Android подключены только для компиляции: во время
 * сборки их классы уже загружены корневым проектом (`apply false`),
 * и второй загрузки, на которую плагин Kotlin жалуется, не происходит.
 */
dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("library") {
            id = "superkassa.library"
            implementationClass = "kz.mybrain.superkassa.gradle.LibraryPlugin"
        }
        register("ios") {
            id = "superkassa.ios"
            implementationClass = "kz.mybrain.superkassa.gradle.IosPlugin"
        }
        register("screens") {
            id = "superkassa.screens"
            implementationClass = "kz.mybrain.superkassa.gradle.ScreensPlugin"
        }
    }
}
