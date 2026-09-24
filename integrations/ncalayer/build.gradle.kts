plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * Подпись ЭЦП через NCALayer — только настольные системы.
 *
 * NCALayer — программа НУЦ РК для Windows, macOS и Linux; на Android и iOS
 * её нет, и цели под них модуль не объявляет. Протокол — запросы, разбор
 * ответов, отказы — в общем коде; вебсокет к NCALayer на петле — в `jvmMain`.
 */
kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.websockets)
        }
    }
}
