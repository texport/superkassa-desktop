import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
}

group = "kz.mybrain.superkassa"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    detektPlugins(libs.detekt.formatting)
}

compose.desktop {
    application {
        mainClass = "kz.mybrain.superkassa.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // Имя бренда латиницей: так касса называется в строке меню,
            // в доке и в списке программ. macOS берёт его из имени пакета —
            // ни заголовок окна, ни `-Xdock:name` его не меняют.
            packageName = "Superkassa"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "kz.mybrain.superkassa.desktop"
                // Значок нарисован из иконки Material 3 задачей `makeIcon`:
                // тот же набор, что и значки в интерфейсе.
                iconFile.set(project.file("icon.icns"))
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Рисует значок приложения из иконки Material 3.
 *
 * Запускается руками при смене значка: `./gradlew makeIcon`. Результат —
 * `icon.png`, из которого собирается `icon.icns` для macOS.
 */
tasks.register<JavaExec>("makeIcon") {
    group = "build"
    mainClass.set("kz.mybrain.superkassa.desktop.tools.IconMakerKt")
    classpath = sourceSets["main"].runtimeClasspath
}
