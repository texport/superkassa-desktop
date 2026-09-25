package kz.mybrain.superkassa.data.local

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random

/**
 * Настройка рабочего места — строка в отдельном файле.
 *
 * По файлу на значение: разбирать один файл со своим форматом ради
 * десятка строк дороже, чем хранить их порознь. Пустое значение стирает
 * файл — отсутствие настройки и пустая настройка это одно и то же.
 */
internal fun readSetting(file: Path): String? = runCatching {
    if (!SystemFileSystem.exists(file)) return@runCatching null
    SystemFileSystem.source(file).buffered().use { it.readString() }.trim().takeIf { it.isNotEmpty() }
}.getOrNull()

/**
 * Записывает настройку заменой файла целиком.
 *
 * Запись идёт через временный файл и атомарный перенос: касса выключается
 * рубильником вместе с розеткой, и наполовину записанная настройка
 * встретила бы кассира утром.
 */
internal fun writeSetting(file: Path, value: String?) {
    runCatching {
        file.parent?.let(SystemFileSystem::createDirectories)
        if (value.isNullOrBlank()) SystemFileSystem.delete(file, mustExist = false) else replace(file, value)
    }
}

/**
 * Заменяет содержимое файла через временный файл рядом с ним.
 *
 * Не вышел перенос — а на Android до 8.0 атомарного переноса нет вовсе, —
 * настройка пишется прямо в файл: лучше так, чем не запомнить её совсем.
 */
private fun replace(file: Path, value: String) {
    val temporary = Path("$file.${Random.nextLong().toULong()}$TEMPORARY")
    write(temporary, value)
    runCatching { SystemFileSystem.atomicMove(temporary, file) }.onFailure {
        SystemFileSystem.delete(temporary, mustExist = false)
        write(file, value)
    }
}

private fun write(file: Path, value: String) = SystemFileSystem.sink(file).buffered().use { it.writeString(value) }

/** Окончание временного файла, пока настройка не перенесена на место. */
private const val TEMPORARY = ".tmp"
