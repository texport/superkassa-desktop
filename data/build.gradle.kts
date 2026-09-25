plugins {
    id("superkassa.library")
}

/**
 * Слой данных кассы: реализации портов домена.
 *
 * Касса — ядро в процессе приложения, внешние службы — модули интеграций,
 * хранение — на устройстве. Модуль переводит их в порты домена и больше
 * ничего не знает: экранов он не видит, экраны не видят его. Собирают
 * адаптеры точки сборки платформенных приложений.
 *
 * Общий код — адаптеры ядра, кабинета и карт; платформе остаётся своё:
 * файлы, печать, журнал и NCALayer на настольных системах, настройки
 * и печать Android.
 *
 * Целей iOS у модуля пока нет, хотя ядро и интеграции их дают: файл
 * журнала в общем коде держит запись `@Synchronized`, а она есть только
 * на JVM. Под iOS нужна своя блокировка и свои адаптеры платформы —
 * это перевод слоя на iOS, а не перенос по модулям.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            // Касса работает в процессе приложения: ядро — библиотека,
            // а не узел за сетью; точка сборки поднимает его сама.
            api(libs.superkassa.core.embedded)
            // Кабинет и карты — модулями интеграций. Их типы стоят
            // в открытых адаптерах (`RemoteCabinet.bfd`, хранилище плиток),
            // и точка сборки отдаёт экземпляр модуля соседним адаптерам.
            api(project(":integrations:bfd-cabinet"))
            api(project(":integrations:maps"))
            // Настройки и журнал рабочего места — файлами на любой платформе.
            api(libs.kotlinx.io.core)
            implementation(libs.kotlinx.atomicfu)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.jna)
            // Выпуски кассы: последний выпуск и установщик под систему.
            implementation(project(":integrations:releases"))
            // Подпись ЭЦП — NCALayer, только на настольных системах.
            api(project(":integrations:ncalayer"))
            // Перенос данных узла в кассу процесса: узел живёт только
            // на настольных машинах, и перенос — тоже.
            api(libs.superkassa.core.import.node)
        }
        androidMain.dependencies {
            // Выбор файла и печать Android идут через окно приложения.
            implementation(libs.androidx.activity)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.superkassa.core.testing)
            implementation(project(":testing"))
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
