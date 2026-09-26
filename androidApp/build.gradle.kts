plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

/**
 * Касса для Android: телефоны и планшеты.
 *
 * Модуль только запускает приложение и собирает его из модулей:
 * экраны — `shared`, адаптеры портов — `data`.
 * Kotlin здесь встроен в AGP 9, отдельного плагина Kotlin не нужно.
 */

/** Версия кассы: по метке выпуска `-PappVersion`, без неё — версия каталога с «-dev». */
val appVersion: String = providers.gradleProperty("appVersion").getOrElse("${libs.versions.appVersion.get()}-dev")

/**
 * Номер сборки Android из чисел версии: 1.2.3 → 1002003.
 *
 * Android ставит поверх установленного только APK с номером не меньше
 * прежнего: номер растёт вместе с меткой выпуска, и помнить его руками
 * не нужно. Суффикс сборки разработчика в номер не входит.
 */
fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = version.substringBefore('-').split('.').map(String::toInt)
    return major * 1_000_000 + minor * 1_000 + patch
}

/**
 * Ключ выпусков Android — файл хранилища и пароли из переменных окружения.
 *
 * На GitHub их кладёт сборка выпуска из секретов репозитория; ключа
 * в репозитории нет. Без них — на машине разработчика — выпускной APK
 * подписывается отладочным ключом и поверх выпуска не встанет.
 */
val releaseStore: String? = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull

android {
    namespace = "kz.mybrain.superkassa"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        applicationId = "kz.mybrain.superkassa"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
    }
    signingConfigs {
        if (releaseStore != null) {
            create("release") {
                storeFile = file(releaseStore)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        // Выпуск сжимается R8: код и ресурсы, которых никто не зовёт, в APK
        // не попадают. Что ядро берёт отражением, перечислено в правилах.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (releaseStore != null) "release" else "debug")
        }
    }
    // Доставку чеков почтой ядро собирает само, на Eclipse Angus Mail, и три
    // его библиотеки несут одинаковые лицензии в META-INF: без выбора одной
    // APK не упаковывается.
    packaging {
        resources {
            pickFirsts += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    // Адаптеры портов собирает точка сборки: экраны слоя данных не видят.
    implementation(project(":data"))
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.process)
}
