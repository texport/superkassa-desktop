plugins {
    id("superkassa.screens")
}

/**
 * Каркас окна кассы: рамка, навигация по разделам, службы окна и сборка
 * моделей областей из портов, которые раздают точки сборки.
 *
 * Каркас — единственный модуль экранов, который видит все области: он
 * ставит их разделы в рамку, заполняет слоты областей соседями — печать,
 * карту, шаги кабинета, карточки среди настроек — и открывает всё это
 * точкам сборки платформенных приложений. Проверки каркаса — окно
 * целиком и сцены, где несколько областей встречаются на одном экране.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Общее экранов и области открыты наружу: точки сборки берут
            // службы окна и наборы портов областей, а зонтичная библиотека
            // iOS видит разделы через каркас.
            api(project(":ui-common"))
            // Навигация окна: ключи разделов — модулем навигации, история
            // «назад» и её отрисовка — Navigation 3 здесь, в каркасе.
            api(project(":navigation"))
            implementation(libs.navigation3.ui)
            api(project(":feature:analytics"))
            api(project(":feature:cabinet"))
            api(project(":feature:debug"))
            api(project(":feature:journal"))
            api(project(":feature:kassa"))
            api(project(":feature:map"))
            api(project(":feature:print"))
            api(project(":feature:settings"))
            api(project(":feature:setup"))
            api(project(":feature:shift"))
            api(project(":feature:update"))
            api(project(":feature:users"))
        }
    }
}
