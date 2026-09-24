package kz.mybrain.superkassa.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Цели iOS модуля-библиотеки: устройство и симулятор на Apple Silicon.
 *
 * Отдельным плагином, а не флагом: цели объявляются при настройке модуля,
 * и модуль, чьи зависимости iOS не дают, просто его не подключает.
 * Собираются цели только на macOS; на прочих машинах сборка их пропускает
 * (`kotlin.native.ignoreDisabledTargets`).
 */
class IosPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(LibraryPlugin::class.java)
        project.extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()
        }
    }
}
