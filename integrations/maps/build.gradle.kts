plugins {
    id("superkassa.ios")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Карты OpenStreetMap: плитки, поиск адреса, место под точкой и город
 * по адресу подключения.
 *
 * Код общий; платформе остаётся только движок Ktor — OkHttp на Android,
 * Darwin на iOS, клиент JDK на настольных системах.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
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
