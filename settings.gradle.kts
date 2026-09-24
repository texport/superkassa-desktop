rootProject.name = "superkassa-desktop"

pluginManagement {
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

// Внешние службы — каждая своим модулем: модуль знает только протокол
// своей службы и не зависит ни от приложения, ни от соседних интеграций.
include(
    ":integrations:bfd-cabinet",
    ":integrations:maps",
    ":integrations:ncalayer",
    ":integrations:releases"
)
