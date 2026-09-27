plugins {
    id("superkassa.ios")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Подпись ЭЦП в приложении eGov mobile — протокол QR- и кросс-подписания.
 *
 * eGov mobile сам забирает данные на подпись и сам отдаёт подпись по HTTPS,
 * а касса сервером не бывает. Между ними — посредник: он принимает данные,
 * выдаёт QR и ссылку запуска eGov mobile и отдаёт подпись долгим запросом.
 * Посредник по умолчанию — публичный базовый API SIGEX; адрес — настройкой.
 *
 * Код общий; платформе остаётся только движок Ktor — OkHttp на Android,
 * Darwin на iOS, клиент JDK на настольных системах.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.java) }
        // Удержанный запрос проверяется движком Android: только OkHttp
        // обрывает соединение по сроку тишины сокета, и дефект виден на нём.
        jvmTest.dependencies { implementation(libs.ktor.client.okhttp) }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}
