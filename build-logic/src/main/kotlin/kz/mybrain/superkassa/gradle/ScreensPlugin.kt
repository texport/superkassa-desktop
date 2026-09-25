package kz.mybrain.superkassa.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Модуль экранов кассы: общее экранов или область приложения.
 *
 * Цели — как у `superkassa.library`, разметка — Compose. Проверки у всех
 * одни: общий код — `kotlin.test` и суммы оснастки `:testing`, JVM — ещё
 * сцена отрисовки оснастки, корутины проверок и настоящие адаптеры: экраны в проверках
 * стоят на кассе поверх ядра, кабинете поверх подставного обмена
 * и настройках на диске, как в приложении. Куча проверок вида — 2 ГБ:
 * сцены держат память до сборки мусора, и полному набору 512 МБ по
 * умолчанию не хватает.
 *
 * Зависимости основного кода модуль называет сам: у общего экранов —
 * домен и дизайн-система, у области — общее экранов. Так граф модулей
 * читается по их файлам сборки.
 */
class ScreensPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        pluginManager.apply(LibraryPlugin::class.java)
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun lib(name: String) = libs.findLibrary(name).get()
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.named("commonTest") {
                dependencies {
                    implementation(lib("kotlin-test"))
                    implementation(project(TESTING))
                }
            }
            sourceSets.named("jvmTest") {
                dependencies {
                    TEST_LIBRARIES.forEach { implementation(lib(it)) }
                    implementation(project(TESTING))
                    implementation(project(DATA))
                }
            }
        }
        tasks.named<Test>("jvmTest") {
            useJUnitPlatform()
            maxHeapSize = HEAP
        }
    }

    private companion object {
        const val TESTING = ":testing"
        const val DATA = ":data"

        /**
         * Библиотеки проверок JVM: утверждения, корутины, стенд ядра и подставной
         * обмен кабинета. Главный поток проверок без подмены — поток Swing,
         * как в окне настольной кассы.
         */
        val TEST_LIBRARIES = listOf(
            "kotlin-test",
            "kotlinx-coroutines-test",
            "kotlinx-coroutines-swing",
            "superkassa-core-testing",
            "ktor-client-mock",
            "ktor-client-content-negotiation",
            "ktor-serialization-json"
        )
        const val HEAP = "2g"
    }
}
