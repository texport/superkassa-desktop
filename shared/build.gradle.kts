plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
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
            dependencies {
                // Предметная область — модулем `domain`: экраны берут его
                // сценарии и модели, а точки сборки — ещё и порты. Открыт
                // наружу: его типы стоят в открытых объявлениях `shared`.
                api(project(":domain"))
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
                // Тексты кассы — модулем `strings`. Открыт наружу: его типы
                // стоят в открытых объявлениях `shared` (язык в `ProvideStrings`,
                // формы текстов в параметрах экранов), и платформенные
                // приложения выбирают язык тем же `Language`.
                api(project(":strings"))
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // «Назад» закрывает наложение так же, как Escape на настольной кассе.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":testing"))
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
