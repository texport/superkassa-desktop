package kz.mybrain.superkassa.desktop.app

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Настройка рабочего места — строка в отдельном файле.
 *
 * По файлу на значение: разбирать один файл со своим форматом ради
 * десятка строк дороже, чем хранить их порознь. Пустое значение стирает
 * файл — отсутствие настройки и пустая настройка это одно и то же.
 */
internal fun readSetting(file: File): String? = runCatching {
    file.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
}.getOrNull()

/**
 * Записывает настройку заменой файла целиком.
 *
 * Запись идёт через временный файл и атомарный перенос: касса выключается
 * рубильником вместе с розеткой, и наполовину записанная настройка
 * встретила бы кассира утром.
 */
internal fun writeSetting(file: File, value: String?) {
    runCatching {
        val directory = file.parentFile
        directory?.mkdirs()
        if (value.isNullOrBlank()) {
            file.delete()
            return@runCatching
        }
        val temporary = File.createTempFile("kkm", ".tmp", directory)
        temporary.writeText(value)
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    }
}
