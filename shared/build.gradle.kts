plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
        commonMain.dependencies {
            // Предметная область — модулем `domain`: экраны берут его
            // сценарии и модели, а точки сборки — ещё и порты. Открыт
            // наружу: его типы стоят в открытых объявлениях `shared`.
            // Слой данных экранам не виден: адаптеры собирают точки
            // сборки платформенных приложений.
            api(project(":domain"))
            // Токены оформления и общие компоненты — модулем `designsystem`.
            // Открыт наружу: тема, выбор оформления и компоненты стоят
            // в открытых объявлениях `shared` и в точках сборки.
            api(project(":designsystem"))
            // Общее экранов — модулем `ui-common`: помощники моделей, общие
            // службы окна, слова домена, контракты между областями. Открыт
            // наружу: его типы стоят в открытых объявлениях каркаса.
            api(project(":ui-common"))
            api(project(":feature:analytics"))
            api(project(":feature:kassa"))
            api(project(":feature:journal"))
            api(project(":feature:shift"))
            api(project(":feature:users"))
            api(project(":feature:map"))
            api(project(":feature:print"))
            api(project(":feature:debug"))
            // Области — по модулю на область; каркас собирает их все и
            // открывает наружу: точки сборки и зонтичная библиотека iOS
            // видят разделы через каркас.
            api(project(":feature:update"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            api(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            // Тексты кассы — модулем `strings`. Открыт наружу: его типы
            // стоят в открытых объявлениях `shared` (язык в `ProvideStrings`,
            // формы текстов в параметрах экранов), и платформенные
            // приложения выбирают язык тем же `Language`.
            api(project(":strings"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":testing"))
        }
        jvmMain.dependencies {
            implementation(libs.compose.desktop)
            implementation(libs.kotlinx.coroutines.swing)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            // Экраны в проверках стоят на настоящих адаптерах: касса поверх
            // ядра, кабинет поверх подставного обмена, настройки на диске.
            implementation(project(":data"))
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
