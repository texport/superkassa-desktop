import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.detekt) apply false
}

/**
 * Предупреждение компилятора Kotlin — ошибка сборки.
 *
 * Действует на каждую компиляцию каждого модуля: общий код, платформы,
 * проверки, инструменты. Предупреждение, которое можно пропустить мимо
 * глаз, копится, и через месяц среди сотни старых не видно нового.
 */
subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}

/**
 * detekt — одними правилами для всех модулей приложения.
 *
 * Разбор идёт по всем исходным наборам модуля — общему, платформенным
 * и проверкам, — а не только по `src/main`, который detekt берёт сам:
 * в мультиплатформенном модуле такого каталога нет, и без этой настройки
 * он молча не проверял бы ничего.
 *
 * Прежние нарушения записаны в `config/detekt/baseline-<модуль>.xml`:
 * новое нарушение падает сразу, а старые снимаются при переводе своей
 * области. Правила свои лежат в `detekt-rules` и проверяют в том числе
 * сам этот модуль.
 */
val detektFormatting = libs.detekt.formatting

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = rootProject.file("config/detekt/baseline-$name.xml")
        source.setFrom(
            provider { file("src").listFiles().orEmpty().map { it.resolve("kotlin") }.filter { it.isDirectory } }
        )
    }
    dependencies {
        add("detektPlugins", detektFormatting)
        add("detektPlugins", dependencies.project(":detekt-rules"))
    }
}
