import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
}

group = "kz.mybrain.superkassa"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jna)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    detektPlugins(libs.detekt.formatting)
}

compose.desktop {
    application {
        mainClass = "kz.mybrain.superkassa.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // Узел едет внутри установщика: см. задачу `bundleNode`.
            appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))
            // Рантайм приложения запускает ещё и узел, а тому нужны модули,
            // которых кассе самой не надо: java.sql для базы, java.naming
            // и java.management для Spring. Урезанный по касс�� рантайм
            // узел не поднимал вовсе.
            includeAllModules = true
            // Имя бренда латиницей: так касса называется в строке меню,
            // в доке и в списке программ. macOS берёт его из имени пакета —
            // ни заголовок окна, ни `-Xdock:name` его не меняют.
            packageName = "Superkassa"
            // Версия установщика — из метки выпуска: файл обязан называть
            // себя сам. У выпуска v1.0.1 установщики звались 1.0.0,
            // и отличить исправленную сборку от той, в которой узел
            // не поднимался, можно было только по дате.
            packageVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
            macOS {
                bundleID = "kz.mybrain.superkassa.desktop"
                // Значок нарисован из иконки Material 3 задачей `makeIcon`:
                // тот же набор, что и значки в интерфейсе.
                iconFile.set(project.file("icon.icns"))
                // Зачем приложению место — этой строкой система спрашивает
                // владельца. Без неё macOS окна разрешения не показывает
                // вовсе и молча отказывает службе геопозиции.
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSLocationWhenInUseUsageDescription</key>
                        <string>Чтобы поставить торговую точку на карте там, где она стоит.</string>
                    """.trimIndent()
                }
                // Подпись настоящим удостоверением, если оно есть в связке.
                //
                // Без него сборка подписывается «на месте» (ad-hoc), и её
                // отпечаток меняется при каждой пересборке: macOS считает
                // приложение каждый раз новым и разрешение на геопозицию,
                // выданное вчера, сегодня уже не действует. С удостоверением
                // требование к приложению строится от сертификата и команды,
                // а не от отпечатка, и разрешение переживает пересборку.
                //
                // Имя берётся из `~/.gradle/gradle.properties` или из
                // `-PmacSigningIdentity=...`; пусто — подписи нет, и сборка
                // всё равно собирается: разработке она не мешает.
                val identity = providers.gradleProperty("macSigningIdentity").orNull.orEmpty()
                signing {
                    sign.set(identity.isNotBlank())
                    if (identity.isNotBlank()) {
                        identity.let(this.identity::set)
                    }
                }
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Рисует значок приложения из иконки Material 3.
 *
 * Запускается руками при смене значка: `./gradlew makeIcon`. Результат —
 * `icon.png`, из которого собирается `icon.icns` для macOS.
 */
tasks.register<JavaExec>("makeIcon") {
    group = "build"
    mainClass.set("kz.mybrain.superkassa.desktop.tools.IconMakerKt")
    classpath = sourceSets["main"].runtimeClasspath
}

/**
 * Кладёт узел в ресурсы приложения.
 *
 * Установщик обязан нести узел с собой: касса без него не работает,
 * а собирать его на машине проверяющего — полдня работы. Файл берётся
 * из `-PnodeJar=<путь>`, иначе из соседнего дерева узла, собранного
 * задачей `:server:bootJar`.
 *
 * Узла рядом может не быть — при разработке кассы он не нужен: узел там
 * свой, запущенный из Gradle, и приложение это видит по отсутствию
 * ресурса.
 */
val nodeJar: Provider<RegularFile> = providers.gradleProperty("nodeJar")
    .map { layout.projectDirectory.file(it) }
    .orElse(provider { newestNodeJar()?.let(layout.projectDirectory::file) })

val bundleNode by tasks.registering(Copy::class) {
    description = "Кладёт узел в ресурсы приложения"
    onlyIf { nodeJar.isPresent }
    from(nodeJar)
    rename { "node.jar" }
    into(layout.buildDirectory.dir("appResources/common"))
}

tasks.matching { it.name.startsWith("prepareAppResources") }.configureEach {
    dependsOn(bundleNode)
}

/** Самый свежий `server-*.jar` из соседнего дерева узла. */
fun newestNodeJar(): String? = rootDir.resolveSibling("superkassa-server")
    .resolve("server/build/libs")
    .listFiles { file -> file.name.startsWith("server-") && file.name.endsWith(".jar") }
    ?.filterNot { it.name.endsWith("-plain.jar") }
    ?.maxByOrNull { it.lastModified() }
    ?.absolutePath

/** Куда собирается рантайм узла: рядом с самим узлом, в ресурсах приложения. */
val nodeRuntimeDir: Provider<Directory> = layout.buildDirectory.dir("appResources/common/node-runtime")

/**
 * Собирает рантайм, которым запускается узел.
 *
 * Свой рантайм приложению jpackage собирает сам, но кладёт его без
 * запускающего файла: в `runtime/.../Home` есть `conf`, `lib` и `legal`
 * и нет `bin`. Запустить узел им нельзя.
 *
 * Держать узел в одной машине с кассой тоже нельзя: касса закрывается
 * посреди смены, а узел обязан это пережить — на том и стоит разделение
 * на две программы. Поэтому узлу собирается свой рантайм, `jlink`
 * от той же Java, которой собрано всё остальное.
 *
 * Рантайм всегда для той системы, где идёт сборка: Windows-установщик
 * собирается на Windows, `deb` — на Linux. Кросс-сборки у `jlink` нет,
 * как и у `jpackage`.
 */
val nodeRuntime by tasks.registering(Exec::class) {
    description = "Собирает рантайм для узла"
    onlyIf { nodeJar.isPresent }
    val output = nodeRuntimeDir.get().asFile
    outputs.dir(output)
    executable = toolchainTool("jlink")
    args(
        "--add-modules", "java.se",
        "--strip-debug", "--no-header-files", "--no-man-pages",
        "--output", output.path
    )
    doFirst { output.deleteRecursively() }
}

tasks.matching { it.name.startsWith("prepareAppResources") }.configureEach {
    dependsOn(nodeRuntime)
    // Раскладка ресурсов снимает право на запуск, и `jlink`-овский
    // `bin/java` приезжает обычным файлом. Возвращаем его здесь; там,
    // где установщик его всё равно потеряет, касса снимает свой список
    // рантайма — см. `LocalNode.ownRuntime`.
    (this as? Copy)?.eachFile {
        if (path.startsWith("node-runtime/bin/")) permissions { unix("755") }
    }
}

/** Путь до средства из той же Java, которой собрано приложение. */
fun toolchainTool(name: String): String {
    val toolchains = extensions.getByType<JavaToolchainService>()
    val home = toolchains.launcherFor(java.toolchain).get().metadata.installationPath.asFile
    val windows = File(home, "bin/$name.exe")
    return if (windows.isFile) windows.path else File(home, "bin/$name").path
}
