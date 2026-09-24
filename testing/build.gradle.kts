plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

/**
 * Оснастка проверок кассы: подделки портов домена, значения ядра
 * и суммы, записанные словами денег.
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
    jvmToolchain(21)

    jvm()
    android {
        namespace = "kz.mybrain.superkassa.testing"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
        }
        jvmMain.dependencies {
            // Суммы кабинета в проверках переводит модуль кабинета — тем же
            // правилом, что и живые ответы.
            implementation(project(":integrations:bfd-cabinet"))
        }
    }
}
