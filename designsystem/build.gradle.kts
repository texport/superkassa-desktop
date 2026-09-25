plugins {
    id("superkassa.ios")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

/**
 * Дизайн-система кассы: токены оформления и общие компоненты Material 3.
 *
 * Токены — шкала отступов и размеров, цвет, шрифты, формы, значки,
 * движение — и сама тема; компоненты — то, что одинаково на экранах
 * всех областей: раскладка по классу окна, кнопки, диалоги, поля,
 * списки, таблицы, карточки разделов, состояния содержимого.
 *
 * Модуль знает только вид: ни домена, ни данных, ни ядра, ни областей
 * приложения. Свои надписи компонентов — «загрузка», «повторить»,
 * «свернуть» — он берёт у модуля текстов через привязку к дереву Compose,
 * которая живёт здесь же.
 *
 * Цели — те же, что у модуля текстов, с iOS: Compose Multiplatform её
 * даёт, и платформенного кода у модуля — по строке на платформу.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Типы Compose стоят в открытых объявлениях: параметры
            // компонентов, токены в `Dp` и `Color`, значки `ImageVector`.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            api(libs.compose.material.icons.extended)
            // Общие аннотации превью стоят на функциях превью всех модулей
            // экранов: Android Studio видит `@Preview` сквозь них.
            api(libs.compose.ui.tooling.preview)
            // Язык и формы текстов — в `ProvideStrings` и `LocalStrings`.
            api(project(":strings"))
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // «Назад» закрывает наложение так же, как Escape на настольной кассе.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            // Сцена отрисовки со средой Compose этой машины, правила размеров
            // и устройства модуля — общие с проверками экранов.
            implementation(project(":testing"))
        }
    }
}

/**
 * Отрисовщик превью Android Studio — только в Android-сборке модуля:
 * путь исполнения библиотеки наружу не отдаётся, и в приложение он не попадает.
 */
dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
