plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

/**
 * Версия приложения, известная ему самому.
 *
 * Установщик называет версию в имени файла, а само приложение её не знало:
 * кассир, звонивший в поддержку, не мог сказать, какая у него касса,
 * а проверка выпусков сравнивать была ни с чем. Метка выпуска приходит
 * в `-PappVersion`, как и для установщика; без неё — сборка разработчика,
 * и она так и называется, чтобы не выдавать себя за выпуск.
 *
 * Версия пишется в общий код: её знает касса на любой платформе.
 */
val appVersion: String = providers.gradleProperty("appVersion").getOrElse("1.0.0-dev")

val versionSourceDir: Provider<Directory> = layout.buildDirectory.dir("generated/version/kotlin")

val generateVersion by tasks.registering {
    description = "Записывает версию приложения в исходный код"
    val output = versionSourceDir.map { it.file("kz/mybrain/superkassa/domain/version/BuildVersion.kt") }
    inputs.property("appVersion", appVersion)
    outputs.dir(versionSourceDir)
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package kz.mybrain.superkassa.domain.version

                /** Версия этой сборки; записывается сборкой из `appVersion`. */
                object BuildVersion {
                    const val NAME: String = "$appVersion"
                }

                """.trimIndent()
            )
        }
    }
}

kotlin {
    jvmToolchain(21)

    // Настольная касса: Windows, Linux, macOS. Код, которому нужны AWT,
    // Swing, файлы и процессы машины, живёт в `jvmMain`.
    jvm()

    android {
        namespace = "kz.mybrain.superkassa.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain {
            // Каталог с версией объявлен задачей, а не путём: так компиляция
            // сама ждёт записи файла, и отдельной зависимости не нужно.
            kotlin.srcDir(generateVersion)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.lifecycle.viewmodel.compose)
                // Касса работает в процессе приложения: ядро — библиотека,
                // а не узел за сетью. Типы его фасада — каноническая модель
                // кассы, и наружу `shared` они выходят как есть: точка сборки
                // в платформенном приложении поднимает ядро сама.
                api(libs.superkassa.core.embedded)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.common)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jna)
            // Перенос данных узла в кассу процесса: узел живёт только
            // на настольных машинах, и перенос — тоже.
            implementation(libs.superkassa.core.import.node)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

/**
 * Проверки вида рисуют экраны сценой Compose в каждом окне и на каждой
 * ступени шрифта, и сцены держат память до сборки мусора. Кучи по
 * умолчанию, 512 МБ, полному набору не хватает: прогон обрывается
 * посреди проверок нехваткой памяти, а не падением проверки.
 */
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    maxHeapSize = "2g"
}
