package kz.mybrain.superkassa.desktop.app.log

import java.io.File

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
 */
class LogFile(
    private val directory: File,
    private val maxBytes: Long = MAX_BYTES,
    private val keep: Int = KEEP
) {

    /** Текущий файл: его открывают первым при разборе. */
    val current: File = File(directory, NAME)

    /**
     * Дописывает строку, переложив файл, если он переполнен.
     *
     * Отказ диска гасится намеренно: непишущийся журнал — не повод
     * прекращать продажу.
     */
    @Synchronized
    fun append(line: String) {
        runCatching {
            directory.mkdirs()
            rotate(line)
            current.appendText(line + "\n")
        }
    }

    /** Файлы журнала: текущий и сохранённые прошлые. */
    fun files(): List<File> = (listOf(current) + (1..keep).map(::previous)).filter { it.exists() }

    private fun rotate(line: String) {
        if (!current.exists()) return
        if (current.length() + line.toByteArray().size + 1 <= maxBytes) return
        previous(keep).delete()
        for (at in keep - 1 downTo 1) {
            previous(at).renameTo(previous(at + 1))
        }
        current.renameTo(previous(1))
    }

    private fun previous(at: Int) = File(directory, "$NAME.$at")

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
