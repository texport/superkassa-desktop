import io.gitlab.arturbosch.detekt.extensions.DetektExtension

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
 * detekt — одними правилами для всех модулей приложения.
 *
 * Разбор идёт по всем исходным наборам модуля — общему, платформенным
 * и проверкам, — а не только по `src/main`, который detekt берёт сам:
 * в мультиплатформенном модуле такого каталога нет, и без этой настройки
 * он молча не проверял бы ничего.
 *
 * Прежние нарушения записаны в `config/detekt/baseline-<модуль>.xml`:
 * новое нарушение падает сразу, а старые снимаются при переводе своей
 * области. Правила свои лежат в `detekt-rules`.
 */
val detektFormatting = libs.detekt.formatting

subprojects {
    if (name == "detekt-rules") return@subprojects
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
        add("detektPlugins", project(":detekt-rules"))
    }
}
