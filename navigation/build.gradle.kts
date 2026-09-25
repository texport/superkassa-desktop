plugins {
    id("superkassa.ios")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Навигация кассы: ключи экранов и то, чем по ним переходят.
 *
 * Как советует Google для Navigation 3, ключи экранов живут отдельно от
 * самих экранов: область открывает соседку по ключу, не видя её кода,
 * а что нарисовать по ключу и где держать историю «назад», решает каркас
 * окна. Модуль маленький — ключи, их реестр для сохранения истории
 * и контракт перехода; ни экранов, ни домена здесь нет.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Ключи — `NavKey` Navigation 3, сохраняются сериализацией.
            api(libs.navigation3.runtime)
            api(libs.kotlinx.serialization.json)
            // Переход отдаётся экранам через дерево Compose.
            api(libs.compose.runtime)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":testing"))
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
