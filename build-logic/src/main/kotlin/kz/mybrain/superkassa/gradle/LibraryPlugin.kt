package kz.mybrain.superkassa.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Модуль-библиотека кассы: общий код, настольные системы и Android.
 *
 * Цели — JVM и Android-библиотека; Android настроен из каталога версий,
 * пространство имён — по пути модуля: `:integrations:bfd-cabinet` →
 * `kz.mybrain.superkassa.integrations.bfdcabinet`. Проверки общего кода
 * идут и на JVM, и на Android. Цели iOS добавляет `superkassa.ios` —
 * там, где их дают зависимости модуля.
 */
class LibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        val versions = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun version(name: String): Int = versions.findVersion(name).get().requiredVersion.toInt()
        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(JAVA)
            jvm()
            extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = ROOT + path.replace(':', '.').replace("-", "")
                compileSdk = version("androidCompileSdk")
                minSdk = version("androidMinSdk")
                withHostTest {}
            }
        }
    }

    private companion object {
        const val JAVA = 21
        const val ROOT = "kz.mybrain.superkassa"
    }
}
