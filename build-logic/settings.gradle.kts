rootProject.name = "build-logic"

// Каталог версий — тот же, что у приложения: версии плагинов не повторяются.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
