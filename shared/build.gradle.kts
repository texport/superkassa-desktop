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
val appVersion: String = providers.gradleProperty("appVersion").getOrElse("${libs.versions.appVersion.get()}-dev")

/**
 * Версия ядра, с которым собрано приложение: её спрашивает поддержка
 * рядом с версией самого приложения, а ядро своей версии не сообщает.
 */
val coreVersion: String = libs.versions.superkassa.core.get()

val versionSourceDir: Provider<Directory> = layout.buildDirectory.dir("generated/version/kotlin")

val generateVersion = tasks.register("generateVersion") {
    description = "Записывает версию приложения в исходный код"
    val output = versionSourceDir.map { it.file("kz/mybrain/superkassa/domain/version/model/BuildVersion.kt") }
    inputs.property("appVersion", appVersion)
    inputs.property("coreVersion", coreVersion)
    outputs.dir(versionSourceDir)
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package kz.mybrain.superkassa.domain.version.model

                /** Версия этой сборки; записывается сборкой из `appVersion`. */
                object BuildVersion {
                    const val NAME: String = "$appVersion"

                    /** Версия ядра, с которым собрана касса. */
                    const val CORE: String = "$coreVersion"
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
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }

    sourceSets {
        commonMain {
            // Каталог с версией объявлен задачей, а не путём: так компиляция
            // сама ждёт записи файла, и отдельной зависимости не нужно.
            kotlin.srcDir(generateVersion)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                // Общий код без java.*: время, файлы и сеть — библиотеками
                // Kotlin Multiplatform; движок сети у каждой платформы свой.
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.core)
                implementation(libs.ktor.client.core)
                // Внешние службы — модулями интеграций: слой `data` переводит
                // их в порты областей.
                implementation(project(":integrations:maps"))
                api(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.navigation.compose)
                // Касса работает в процессе приложения: ядро — библиотека,
                // а не узел за сетью. Типы его фасада — каноническая модель
                // кассы, и наружу `shared` они выходят как есть: точка сборки
                // в платформенном приложении поднимает ядро сама.
                api(libs.superkassa.core.embedded)
                // Кабинет БФД — модулем интеграции; экземпляр модуля открыт
                // наружу `data` (`RemoteCabinet.bfd`), и точка сборки отдаёт
                // его соседним адаптерам того же кабинета.
                api(project(":integrations:bfd-cabinet"))
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // «Назад» закрывает наложение так же, как Escape на настольной кассе.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.compose.desktop)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jna)
            // Выпуски кассы: последний выпуск и установщик под систему.
            implementation(project(":integrations:releases"))
            // Подпись ЭЦП — NCALayer, только на настольных системах.
            implementation(project(":integrations:ncalayer"))
            // Перенос данных узла в кассу процесса: узел живёт только
            // на настольных машинах, и перенос — тоже.
            implementation(libs.superkassa.core.import.node)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            // Тестовый БФД и часы проверки из оснастки ядра: касса в процессе
            // заводится и пробивает чеки без сети и без своего двойника БФД.
            implementation(libs.superkassa.core.testing)
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
