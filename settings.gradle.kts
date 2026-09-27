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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/**
 * Версия из каталога `gradle/libs.versions.toml` — до того, как Gradle его прочтёт.
 */
fun catalogVersion(key: String): String =
    file("gradle/libs.versions.toml").readLines().firstNotNullOf { line ->
        Regex("""^$key\s*=\s*"(.+)"\s*$""").find(line)?.groupValues?.get(1)
    }

/**
 * Готовая сборка нашей библиотеки из её выпуска на GitHub.
 *
 * Выпуск несёт архив `<архив>-maven-<версия>.zip` — собранную библиотеку
 * для всех целей с метаданными Gradle. Он раскладывается в локальный Maven
 * один раз на версию; дальше сборка берёт библиотеку оттуда, как и прежде.
 * Собирать соседей из исходников не нужно ни на GitHub, ни у разработчика.
 * Версию `-SNAPSHOT` не скачивают: это своя локальная сборка библиотеки.
 *
 * @param repository репозиторий texport с выпусками.
 * @param artifact артефакт, по которому видно, что версия уже разложена.
 */
fun releasedLibrary(repository: String, artifact: String, version: String) {
    if (version.endsWith("-SNAPSHOT")) return
    val local = File(System.getProperty("user.home"), ".m2/repository").canonicalFile
    if (File(local, "io/github/texport/$artifact/$version").isDirectory) return
    val address = "https://github.com/texport/$repository/releases/download/v$version/$repository-maven-$version.zip"
    val unpacked = File(local, ".superkassa-unpacking").apply { deleteRecursively(); mkdirs() }
    java.util.zip.ZipInputStream(uri(address).toURL().openStream().buffered()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
            val target = File(unpacked, entry.name).canonicalFile
            require(target.startsWith(unpacked)) { "Файл вне архива: ${entry.name}" }
            target.parentFile.mkdirs()
            target.outputStream().use { zip.copyTo(it) }
        }
    }
    // Уже лежащее не трогается: список версий артефакта в локальном Maven
    // знает и свои сборки, а архив — только свою версию.
    unpacked.copyRecursively(local, overwrite = false) { _, _ -> OnErrorAction.SKIP }
    unpacked.deleteRecursively()
}

// Ядро несёт в своём архиве и кодек с протоколом, с которыми оно собрано.
releasedLibrary("superkassa-core", "superkassa-core-embedded", catalogVersion("superkassa-core"))

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

// Навигация — ключи экранов и то, чем по ним переходят: область открывает
// соседку по ключу, не видя её самой; историю «назад» держит каркас окна.
include(":navigation")

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
include(":feature:setup")
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
    ":integrations:egov-mobile",
    ":integrations:kalkan",
    ":integrations:maps",
    ":integrations:ncalayer",
    ":integrations:releases"
)
