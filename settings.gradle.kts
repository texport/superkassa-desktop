rootProject.name = "superkassa-desktop"

pluginManagement {
    // Общая настройка модулей-библиотек — плагинами `build-logic`.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

// Раскладка — по шаблону Compose Multiplatform для AGP 9: общий код
// живёт библиотекой, у каждой платформы — своё приложение поверх неё.
// `detekt-rules` — свои правила проверки, в приложение не входят.
include(":shared", ":desktopApp", ":androidApp", ":detekt-rules")

// Тексты кассы на трёх языках — своим модулем: в нём только слова и доступ
// к ним, без Compose, ядра и домена; приложение берёт их через `api`.
include(":strings")

// Дизайн-система — токены оформления и общие компоненты Material 3:
// знает только вид и тексты, без домена, данных и областей приложения.
include(":designsystem")

// Предметная область кассы — модели, сценарии и порты на чистом Kotlin:
// знает только типы ядра и не видит ни экранов, ни хранения, ни служб.
include(":domain")

// Общее экранов — то, что берут несколько областей и что знает домен:
// помощники моделей, деньги, сообщения, слова домена, контракты между
// областями. Областей модуль не знает, области друг друга — тоже.
include(":ui-common")

// Области экранов — по модулю на область: область видит общее экранов,
// дизайн-систему, тексты и домен, но ни одной соседней области. Области
// собирает каркас окна — модуль `shared`.
include(":feature:analytics")
include(":feature:cabinet")
include(":feature:debug")
include(":feature:journal")
include(":feature:kassa")
include(":feature:map")
include(":feature:print")
include(":feature:settings")
include(":feature:shift")
include(":feature:update")
include(":feature:users")

// Слой данных — реализации портов домена: ядро в процессе, интеграции,
// хранение на устройстве. Экраны его не видят; собирают точки сборки.
include(":data")

// Оснастка проверок: подделки портов и значения ядра, общие для проверок
// всех модулей. В приложение не входит.
include(":testing")

// Внешние службы — каждая своим модулем: модуль знает только протокол
// своей службы и не зависит ни от приложения, ни от соседних интеграций.
include(
    ":integrations:bfd-cabinet",
    ":integrations:maps",
    ":integrations:ncalayer",
    ":integrations:releases"
)
