plugins {
    id("superkassa.ios")
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
