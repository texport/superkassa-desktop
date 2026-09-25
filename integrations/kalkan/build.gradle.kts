plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

/**
 * Подпись файлом ключа ЭЦП — провайдером Kalkan НУЦ РК.
 *
 * Ключи НУЦ РК — PKCS#12 с алгоритмами ГОСТ на казахстанских OID, которых
 * нет ни в Android, ни в BouncyCastle; знает их только Kalkan. Kalkan —
 * библиотека Java, поэтому у модуля одна цель — Android: на компьютере
 * подписывает NCALayer. Проверки идут на JVM машины разработчика
 * (`withHostTest`) — Kalkan там тот же.
 *
 * Kalkan в Maven Central не публикуется: НУЦ выдаёт его разработчикам,
 * и сборка берёт его из локального хранилища Maven.
 */
kotlin {
    jvmToolchain(21)

    android {
        namespace = "kz.mybrain.superkassa.integrations.kalkan"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            api(libs.kalkan.provider)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
