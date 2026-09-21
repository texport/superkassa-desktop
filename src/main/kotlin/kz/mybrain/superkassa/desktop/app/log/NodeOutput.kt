package kz.mybrain.superkassa.desktop.app.log

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Вывод узла в том же журнале, что и всё остальное.
 *
 * Цепочка обрывалась на границе с узлом: приложение писало «POST
 * …/shift/close -> 200 за 155 мс», а что узел при этом отправил в ОФД
 * и что получил — лежало в его собственном файле, о котором надо знать.
 * Разбор отказа означал сложить два файла по времени руками, а на чужой
 * машине — ещё и найти второй.
 *
 * Узел запускается вместе с кассой и пишет вывод рядом с настройками
 * рабочего места. Здесь этот файл дочитывается и укладывается в общий
 * журнал: в окне отладки видно и обращение приложения, и обмен узла
 * с ОФД одной лентой.
 *
 * Читается только в режиме отладки: в обычной работе узел пишет
 * в свой файл, и переписывать его во второй незачем.
 */
class NodeOutput(private val file: File, private val journal: LogJournal) {

    /**
     * Дочитывает файл узла, пока чтение не отменят.
     *
     * Своего потока не держит: живёт тем, кто его позвал, и умирает
     * вместе с ним — отладку выключили, чтение кончилось.
     */
    suspend fun follow() {
        // Хвост файла, а не весь: при включении отладки посреди работы
        // прежние сотни строк в окно не нужны, нужен обмен с этого места.
        var read = file.takeIf { it.isFile }?.length() ?: 0L
        while (currentCoroutineContext().isActive) {
            read = pass(read)
            delay(PAUSE_MS)
        }
    }

    /**
     * Дочитывает появившееся с прошлого раза.
     *
     * Узел мог перезапуститься и начать файл заново — тогда он короче
     * прочитанного, и чтение начинается с начала.
     *
     * @return сколько прочитано теперь.
     */
    private fun pass(read: Long): Long {
        val length = runCatching { file.takeIf { it.isFile }?.length() }.getOrNull() ?: return read
        if (length == read) return read
        val from = if (length < read) 0L else read
        val added = runCatching { readFrom(from) }.getOrNull() ?: return read
        added.lineSequence().filter { it.isNotBlank() }.forEach(::keep)
        return length
    }

    private fun readFrom(from: Long): String = file.inputStream().use { stream ->
        stream.skip(from)
        stream.readBytes().decodeToString()
    }

    /**
     * Укладывает строку узла в журнал.
     *
     * Время и уровень у узла свои, и переставлять их незачем: строка
     * кладётся как есть, а уровень определяется её же словами — иначе
     * отбор «только отказы» прошёл бы мимо отказов узла.
     */
    private fun keep(line: String) =
        journal.record(LogSource.NodeSelf, levelOf(line), line.trim())

    /**
     * Насколько важна строка узла.
     *
     * Обычная идёт уровнем обращения, а не отладочным: чтение включено
     * только в режиме отладки, и прятать прочитанное ещё и за уровнем
     * значило бы включить отладку и ничего не увидеть. Уровень
     * определяется словами самой строки — иначе отбор «только отказы»
     * прошёл бы мимо отказов узла.
     */
    private fun levelOf(line: String): LogLevel = when {
        FAILURES.any { line.contains(it) } -> LogLevel.Failure
        line.contains(" WARN") -> LogLevel.Warning
        else -> LogLevel.Info
    }

    private companion object {

        /** Как часто смотреть, не дописал ли узел. */
        const val PAUSE_MS = 400L

        /** Слова, которыми узел сообщает об отказе. */
        val FAILURES = listOf(" ERROR", "Exception", "Unhandled")
    }
}
