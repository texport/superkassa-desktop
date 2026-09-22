package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.releases.Release
import kz.mybrain.superkassa.desktop.server.releases.ReleaseAsset

/**
 * Какой файл выпуска ставится на эту систему.
 *
 * Выпуск несёт три установщика — под macOS, Windows и Debian, — и кассиру
 * нужен ровно один, свой. Опознаётся по расширению файла, а система —
 * по её имени в Java: другого способа у настольного приложения нет.
 */
enum class Installer(val extension: String, private val systemName: String) {
    Mac(".dmg", "mac"),
    Windows(".msi", "windows"),
    Debian(".deb", "linux");

    companion object {
        /** Установщик под систему с таким `os.name`; неизвестная система — без установщика. */
        fun forSystem(osName: String?): Installer? {
            val lowered = osName?.lowercase() ?: return null
            return entries.firstOrNull { lowered.contains(it.systemName) }
        }
    }
}

/**
 * Ссылка на установщик этого выпуска для этой системы.
 *
 * Нет подходящего файла — нет ссылки: тогда кассиру открывают страницу
 * выпуска, где он выберет сам.
 */
fun Release.installerFor(installer: Installer?): ReleaseAsset? =
    installer?.let { wanted -> assets.firstOrNull { it.name.endsWith(wanted.extension, ignoreCase = true) } }
