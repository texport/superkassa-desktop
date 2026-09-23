import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

/**
 * Свои правила detekt.
 *
 * detekt исполняет их своим компилятором Kotlin 2.0, поэтому и собираются
 * они под язык 2.0: код под более новую стандартную библиотеку мог бы
 * не найти в ней нужного при разборе.
 */
kotlin {
    jvmToolchain(21)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    compileOnly(libs.detekt.api)
}
