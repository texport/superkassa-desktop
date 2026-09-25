package kz.mybrain.superkassa.data.log

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString

/**
 * Журнал на диске: текущий файл и несколько прошлых.
 *
 * Файл живёт рядом с остальными настройками рабочего места. Размер и число
 * файлов ограничены: касса работает годами, а журнал пишется каждую смену —
 * без ограничения он однажды занял бы диск целиком, и первым это заметил бы
 * кассир посреди продажи, а не обслуживание.
 *
 * Прошлые файлы нумеруются от свежего к старому: `superkassa.log.1` — тот,
 * что закончился последним. Переполнение сдвигает номера, самый старый
 * пропадает.
 *
 * Одни правила на всех платформах: на компьютере файл лежит в каталоге
 * рабочего места, на Android — в каталоге приложения.
 */
class LogFile(
    private val directory: Path,
    private val maxBytes: Long = MAX_BYTES,
    private val keep: Int = KEEP
) {

    /** Текущий файл: его открывают первым при разборе. */
    val current: Path = Path(directory, NAME)

    /** Строки пишутся из разных потоков: перекладка и дозапись идут по одной. */
    private val lock = reentrantLock()

    /**
     * Дописывает строку, переложив файл, если он переполнен.
     *
     * Отказ диска гасится намеренно: непишущийся журнал — не повод
     * прекращать продажу.
     */
    fun append(line: String) {
        lock.withLock {
            runCatching {
                SystemFileSystem.createDirectories(directory)
                val text = line + "\n"
                rotate(text.encodeToByteArray().size)
                SystemFileSystem.sink(current, append = true).buffered().use { it.writeString(text) }
            }
        }
    }

    /** Файлы журнала: текущий и сохранённые прошлые. */
    fun files(): List<Path> = (listOf(current) + (1..keep).map(::previous)).filter(SystemFileSystem::exists)

    private fun rotate(adding: Int) {
        val size = SystemFileSystem.metadataOrNull(current)?.size ?: return
        if (size + adding <= maxBytes) return
        SystemFileSystem.delete(previous(keep), mustExist = false)
        for (at in keep - 1 downTo 1) {
            if (SystemFileSystem.exists(previous(at))) SystemFileSystem.atomicMove(previous(at), previous(at + 1))
        }
        SystemFileSystem.atomicMove(current, previous(1))
    }

    private fun previous(at: Int) = Path(directory, "$NAME.$at")

    companion object {

        /** Имя текущего файла журнала. */
        const val NAME: String = "superkassa.log"

        /**
         * Сколько весит один файл.
         *
         * Полмегабайта — это несколько тысяч строк обмена: смены хватает
         * с запасом, а переслать такой файл в поддержку можно письмом.
         */
        private const val MAX_BYTES = 512L * 1024

        /** Сколько прошлых файлов хранится, кроме текущего. */
        private const val KEEP = 3
    }
}
