import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("superkassa.ios")
    alias(libs.plugins.compose.compiler)
}

/**
 * Оснастка проверок кассы: подделки портов домена, значения ядра,
 * касса поверх поддельного и настоящего ядра, подставной кабинет
 * суммы, записанные словами денег, сцена отрисовки экранов и правила
 * устройства модулей по их исходникам.
 *
 * Проверки домена, данных и экранов живут в своих модулях, а говорят
 * об одной и той же кассе: одна подделка порта на всех, а не копия
 * в каждом модуле, которая однажды разойдётся с остальными. Наборы
 * проверок берут модуль зависимостью проверок; в приложение он не входит.
 *
 * Общий код — только то, что нужно общим проверкам на каждой платформе;
 * подделки, которые пишутся на JVM, — в `jvmMain`.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
        }
        jvmMain.dependencies {
            // Касса поверх поддельного фасада ядра и поверх настоящего ядра
            // стенда — тем же адаптером, что в приложении. Проверкам домена
            // адаптер не виден: они получают кассу портом домена.
            implementation(project(":data"))
            api(libs.superkassa.core.testing)
            // Суммы кабинета и обмен с ним в проверках — модулем кабинета,
            // тем же правилом, что и живые ответы.
            api(project(":integrations:bfd-cabinet"))
            api(project(":integrations:ncalayer"))
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            // Сцена отрисовки без окна — в теме, надписях и классе окна
            // дизайн-системы, как в окне кассы. Ею проверяют и компоненты
            // дизайн-системы, и экраны приложения.
            api(project(":designsystem"))
        }
    }
}

/**
 * Составные функции есть только у сцены отрисовки, а она — только на JVM:
 * компилятор Compose нужен одной этой цели, и общему коду и прочим целям
 * оснастки среда Compose не навязывается.
 */
composeCompiler {
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.jvm))
}
