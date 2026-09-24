import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

group = "kz.mybrain.superkassa"
version = libs.versions.appVersion.get()

/**
 * Имя бренда: им названы пакет, группа меню и ярлык во всех трёх системах.
 *
 * Объявлено здесь, а не повторено в каждом блоке: у настроек Linux есть
 * своё поле `packageName`, и `menuGroup = packageName` внутри блока молча
 * брало его — пустое, — а не имя пакета.
 */
val appName = "Superkassa"

kotlin {
    jvmToolchain(21)
}

/**
 * Инструменты разработчика — своим набором исходников.
 *
 * Значок рисуется руками и кассиру не нужен: из `main` он уезжал
 * в установщик. Отдельный набор видит то же, что приложение, а в упаковку
 * не попадает.
 */
val tools: SourceSet = sourceSets.create("tools") {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

dependencies {
    implementation(project(":shared"))
    // Точка сборки заводит клиентов внешних служб сама.
    implementation(project(":integrations:maps"))
    implementation(project(":integrations:bfd-cabinet"))
    // Точка сборки переносит данные прежнего узла до того, как поднять кассу.
    implementation(libs.superkassa.core.import.node)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.kotlin.test)
    add(tools.implementationConfigurationName, project(":shared"))
    add(tools.implementationConfigurationName, compose.desktop.currentOs)
    add(tools.implementationConfigurationName, libs.compose.material3)
    add(tools.implementationConfigurationName, libs.compose.material.icons.extended)
}

compose.desktop {
    application {
        mainClass = "kz.mybrain.superkassa.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // Касса работает в процессе приложения: ядро и перенос данных
            // прежнего узла тянут базу, сеть и журнал, и нужные им модули
            // Java по зависимостям не угадать. Недостающий модуль всплыл бы
            // у кассира отказом запуска, а не при сборке.
            includeAllModules = true
            // Имя бренда латиницей: так касса называется в строке меню,
            // в доке и в списке программ. macOS берёт его из имени пакета —
            // ни заголовок окна, ни `-Xdock:name` его не меняют.
            packageName = appName
            // Версия установщика — из метки выпуска: файл обязан называть
            // себя сам. У выпуска v1.0.1 установщики звались 1.0.0,
            // и отличить исправленную сборку от прежней можно было
            // только по дате.
            packageVersion = providers.gradleProperty("appVersion").getOrElse(libs.versions.appVersion.get())
            macOS {
                // Номер пакета для macOS остаётся прежним, хотя пакеты кода
                // переименованы: по нему система узнаёт установленную кассу
                // и хранит её разрешения, новый номер был бы другой программой.
                bundleID = "kz.mybrain.superkassa.desktop"
                // Значок нарисован из иконки Material 3 задачей `makeIcon`:
                // тот же набор, что и значки в интерфейсе.
                iconFile.set(rootProject.file("icon.icns"))
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
                iconFile.set(rootProject.file("icon.ico"))
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
                iconFile.set(rootProject.file("icon.png"))
                // Запись в меню приложений — тем же способом, что и ярлык
                // в Windows: без неё установленная касса не находится
                // поиском по программам.
                shortcut = true
                menuGroup = appName
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

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
    from(rootProject.layout.projectDirectory.file("icon.png"))
}

/**
 * Рисует значок приложения из иконки Material 3.
 *
 * Запускается руками при смене значка: `./gradlew makeIcon`. Результат —
 * `icon.png` в корне хранилища; он же идёт в Linux-установщик и в окно
 * приложения, а из него собираются `icon.icns` для macOS и `icon.ico`
 * для Windows.
 */
tasks.register<JavaExec>("makeIcon") {
    group = "build"
    mainClass.set("kz.mybrain.superkassa.tools.IconMakerKt")
    classpath = tools.runtimeClasspath
    workingDir = rootDir
}
