plugins {
    id("superkassa.ios")
}

/**
 * Слой данных кассы: реализации портов домена.
 *
 * Касса — ядро в процессе приложения, внешние службы — модули интеграций,
 * хранение — на устройстве. Модуль переводит их в порты домена и больше
 * ничего не знает: экранов он не видит, экраны не видят его. Собирают
 * адаптеры точки сборки платформенных приложений.
 *
 * Общий код — адаптеры ядра, кабинета и карт, настройки и журнал рабочего
 * места; платформе остаётся только то, что она делает иначе: окна системы,
 * печать, NCALayer и место машины на настольных системах; окно «Сохранить»,
 * выбор файла ключа и подпись им, печать, журнал системы и язык приложения
 * на Android.
 *
 * Цели iOS — как у домена и интеграций: ядро, кабинет и карты их дают,
 * а общий код слоя стоит только на kotlinx-io, kotlinx-datetime и atomicfu.
 * Своих адаптеров платформы под iOS пока нет — их добавит приложение iOS.
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
            // Подпись в eGov mobile — общим кодом: протокол посредника
            // не зависит от платформы.
            api(project(":integrations:egov-mobile"))
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
            // Подпись файлом ключа НУЦ РК — провайдером Kalkan, только Android:
            // на компьютере подписывает NCALayer.
            implementation(project(":integrations:kalkan"))
        }
        // Подпись файлом ключа проверяется на JVM машины разработчика:
        // Kalkan там тот же, что на Android.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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
