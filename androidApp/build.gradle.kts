plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

/**
 * Касса для Android: телефоны и планшеты.
 *
 * Модуль только запускает приложение — весь код живёт в `shared`.
 * Kotlin здесь встроен в AGP 9, отдельного плагина Kotlin не нужно.
 */
android {
    namespace = "kz.mybrain.superkassa"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        applicationId = "kz.mybrain.superkassa"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = providers.gradleProperty("appVersion").getOrElse("${libs.versions.appVersion.get()}-dev")
    }
    buildTypes {
        // Выпуск сжимается R8: код и ресурсы, которых никто не зовёт, в APK
        // не попадают. Что ядро берёт отражением, перечислено в правилах.
        //
        // Подпись пока отладочная: ключа владельца в сборке нет, а выпускной
        // APK нужно ставить на устройство и проверять. Ключ владельца
        // заменит эту строку, когда появится.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.process)
    // Касса отладочной сборки — на тестовом БФД в процессе (`src/debug`):
    // оснастка ядра в выпуск не попадает.
    debugImplementation(libs.superkassa.core.testing)
}
