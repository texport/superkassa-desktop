plugins {
    id("superkassa.screens")
}

/**
 * Журнал рабочего места и режим отладки: карточка среди настроек,
 * соседнее окно журнала на компьютере и окно журнала поверх кассы
 * на Android.
 *
 * Область знает общее экранов, дизайн-систему, тексты и домен — и ни одной
 * соседней области: карточку ставят настройки, окно журнала — точки входа
 * платформ.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":ui-common"))
        }
        jvmMain.dependencies {
            // Соседнее окно журнала — окно настольного Compose.
            implementation(libs.compose.desktop)
        }
    }
}
