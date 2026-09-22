import java.util.concurrent.Callable
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

/**
 * Версия приложения, известная ему самому.
 *
 * Установщик называет версию в имени файла, а само приложение её не знало:
 * кассир, звонивший в поддержку, не мог сказать, какая у него касса,
 * а проверка выпусков сравнивать была ни с чем. Метка выпуска приходит
 * в `-PappVersion`, как и для установщика; без неё — сборка разработчика,
 * и она так и называется, чтобы не выдавать себя за выпуск.
 */
val appVersion: String = providers.gradleProperty("appVersion").getOrElse("1.0.0-dev")

/**
 * Имя бренда: им названы пакет, группа меню и ярлык во всех трёх системах.
 *
 * Объявлено здесь, а не повторено в каждом блоке: у настроек Linux есть
 * своё поле `packageName`, и `menuGroup = packageName` внутри блока молча
 * брало его — пустое, — а не имя пакета.
 */
val appName = "Superkassa"

val versionSourceDir: Provider<Directory> = layout.buildDirectory.dir("generated/version/kotlin")

val generateVersion by tasks.registering {
    description = "Записывает версию приложения в исходный код"
    val output = versionSourceDir.map { it.file("kz/mybrain/superkassa/desktop/app/BuildVersion.kt") }
    inputs.property("appVersion", appVersion)
    outputs.dir(versionSourceDir)
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package kz.mybrain.superkassa.desktop.app

                /** Версия этой сборки; записывается сборкой из `appVersion`. */
                object BuildVersion {
                    const val NAME: String = "$appVersion"
                }

                """.trimIndent()
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
    // Каталог с версией объявлен задачей, а не путём: так компиляция сама
    // ждёт записи файла, и отдельной зависимости между задачами не нужно.
    sourceSets["main"].kotlin.srcDir(generateVersion)
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
            packageName = appName
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
                    // Языки кассы объявлены бандлом: без этого macOS считает
                    // приложение англоязычным и показывает свои диалоги —
                    // сохранение файла, печать — по-английски, тогда как сама
                    // касса говорит с кассиром по-русски или по-казахски.
                    extraKeysRawXml = """
                        <key>NSLocationWhenInUseUsageDescription</key>
                        <string>Чтобы поставить торговую точку на карте там, где она стоит.</string>
                        <key>CFBundleDevelopmentRegion</key>
                        <string>ru</string>
                        <key>CFBundleLocalizations</key>
                        <array>
                            <string>kk</string>
                            <string>ru</string>
                            <string>en</string>
                        </array>
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
            windows {
                // Значок для Windows — только `.ico`: `.icns` jpackage там
                // не понимает, и без этой строки установленная касса
                // получала общий значок программы на Java. Файл лежит
                // в репозитории собранным: на сборочной машине рисовать
                // его нечем.
                iconFile.set(project.file("icon.ico"))
                // Касса ставится и пропадает: установщик не делал ни записи
                // в меню «Пуск», ни ярлыка, и найти её можно было только
                // обходом папки установки. Группа меню названа программой —
                // отдельного имени у неё нет.
                menu = true
                menuGroup = appName
                shortcut = true
                // Постоянный номер продукта для MSI. Установщик отличает
                // обновление от новой программы только по нему: без номера
                // WiX выдаёт новый при каждой сборке, и выпуск вставал
                // второй копией рядом со старой вместо замены.
                //
                // Менять эту строку нельзя никогда: смена номера означает
                // для Windows другую программу, и следующий выпуск снова
                // встанет рядом, а не поверх.
                upgradeUuid = "1A5AA245-E0C5-405E-9226-F1747F83737B"
            }
            linux {
                // Для Linux jpackage берёт `.png` — тот же, из которого
                // собраны остальные значки.
                iconFile.set(project.file("icon.png"))
                // Запись в меню приложений — тем же способом, что и ярлык
                // в Windows: без неё установленная касса не находится
                // поиском по программам.
                shortcut = true
                menuGroup = appName
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
 * `icon.png`; он же идёт в Linux-установщик и в окно приложения,
 * а из него собираются `icon.icns` для macOS и `icon.ico` для Windows.
 */
/**
 * Кладёт значок в ресурсы приложения.
 *
 * Значок нужен не только установщику: окно называет его себе само, иначе
 * в панели задач Windows рядом с кассой стоит общий значок программы
 * на Java. Берётся тот же файл, что и для установщиков, — копия
 * в исходниках была бы вторым значком, который однажды разойдётся
 * с первым.
 */
tasks.processResources {
    from(layout.projectDirectory.file("icon.png"))
}

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
    .map { named ->
        // Узел назвали, но его там нет — собирать установщик без узла
        // нельзя: он поставится и молча не заработает. Пустая задача
        // копирования об этом не скажет, поэтому проверка здесь.
        require(File(named).isFile) { "узел не найден: $named" }
        layout.projectDirectory.file(named)
    }
    .orElse(provider { newestNodeJar()?.let(layout.projectDirectory::file) })

val bundleNode by tasks.registering(Copy::class) {
    description = "Кладёт узел в ресурсы приложения"
    onlyIf { nodeJar.isPresent }
    // Источник берётся отложенно: без узла провайдер пуст, и обращение
    // к нему при построении графа задач ломало сборку целиком — раньше
    // самой проверки `requireNode`, с невнятным «has no value available».
    from(Callable { nodeJar.orNull ?: emptyList<Any>() })
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

/**
 * Не даёт собрать установщик без узла.
 *
 * Установщик без узла ставится и не работает: касса показывает «Узел
 * не на связи», и понять причину можно только по журналу. В чистом клоне
 * соседнего дерева узла нет, и молчаливый пропуск давал ровно такую сборку.
 *
 * Осознанная сборка без узла — `-PwithoutNode`.
 */
val requireNode by tasks.registering {
    description = "Проверяет, что узел есть"
    doLast {
        require(nodeJar.isPresent || providers.gradleProperty("withoutNode").isPresent) {
            "узла нет: соберите его в ../superkassa-server (./gradlew :server:bootJar), " +
                "укажите -PnodeJar=<путь> или соберите без узла с -PwithoutNode"
        }
    }
}

tasks.matching { it.name == "createDistributable" || it.name.startsWith("package") }.configureEach {
    dependsOn(requireNode)
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
