plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
 * Версия — модель домена: её знает касса на любой платформе.
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

/**
 * Предметная область кассы: модели, сценарии и порты.
 *
 * Чистый Kotlin в общем коде — ни Compose, ни хранения, ни сети, ни
 * модулей интеграций. Типы ядра — каноническая модель кассы: домен
 * говорит ими и открывает их наружу как есть, своих двойников не заводит.
 * Реализации портов живут в `data`, экраны — в `shared`; сюда не смотрит
 * ни тот, ни другой слой, и это держит граф модулей, а не договорённость.
 *
 * Цели те же, что у модулей интеграций: ядро выпускает свои типы и под
 * iOS, и домен собирается под неё без единой платформенной строки.
 */
kotlin {
    jvmToolchain(21)

    jvm()
    android {
        namespace = "kz.mybrain.superkassa.domain"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            // Каталог с версией объявлен задачей, а не путём: так компиляция
            // сама ждёт записи файла, и отдельной зависимости не нужно.
            kotlin.srcDir(generateVersion)
            dependencies {
                // Типы ядра, потоки, даты и разбор JSON ответов ОФД стоят
                // в открытых объявлениях домена — потребитель получает их с ним.
                api(libs.superkassa.core.presentation)
                api(libs.superkassa.core.domain)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":testing"))
        }
        jvmTest.dependencies {
            // Проверки названий на трёх языках рядом с правилом, которое
            // их выбирает; самому домену тексты не нужны.
            implementation(project(":strings"))
            implementation(libs.jakarta.validation.api)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
