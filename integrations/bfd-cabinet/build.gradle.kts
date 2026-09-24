plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Кабинет БФД (ECC): вход по ЭЦП, компания, точки, кассы, заявления в ИСНА,
 * документы касс и аналитика — протокол кабинета, его DTO и ошибки.
 *
 * Код общий; платформе остаётся только движок Ktor — OkHttp на Android,
 * Darwin на iOS, клиент JDK на настольных системах.
 */
kotlin {
    jvmToolchain(21)

    jvm()
    android {
        namespace = "kz.mybrain.superkassa.integrations.bfdcabinet"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.java) }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}
