package kz.mybrain.superkassa.data.kassa

import io.github.texport.superkassa.importnode.api.NodeImportException
import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import java.io.File

/**
 * Перенос данных прежнего узла в кассу процесса — при каждом запуске, до кассы.
 *
 * Первый запуск переносит кассы, смены, документы, очередь и кассиров
 * узла; следующие отвечают, что перенос уже сделан, и ничего не трогают.
 * На машине, где узла не было, переносить нечего, и касса начинает
 * с чистого каталога.
 */
object NodeDataMove {

    /**
     * Переносит данные узла.
     *
     * @param nodeHome рабочее место узла: там его настройки и база — каталог
     *   данных кассы, который узлу называли свойством `superkassa.home`.
     * @param kassaDir каталог кассы процесса — тот же, на котором она поднимется.
     * @param nodeAddress адрес узла: ответивший узел ещё пишет, и перенос не начинается.
     * @return что стало с данными узла — или почему касса не открывается.
     */
    fun run(nodeHome: File, kassaDir: File, nodeAddress: String?): Result<NodeImportResult> =
        runCatching { importNodeData(nodeHome.path, kassaDir.path, nodeAddress) }

    /**
     * Причина отказа словами кассира.
     *
     * Ядро называет причину текстом для обслуживания; здесь он разводится
     * по тому, что делать: закрыть узел, закрыть второе окно кассы или звать
     * обслуживание. Незнакомая причина — к обслуживанию: касса не открыта,
     * и гадать о ней нельзя. Сбой не переноса, а самого каталога кассы —
     * его не удалось создать — называется каталогом.
     */
    fun problemOf(failure: Throwable): StartProblem {
        val detail = failure.message ?: failure::class.simpleName.orEmpty()
        val refusal = when {
            failure !is NodeImportException -> StartRefusal.KassaNotOpened
            NODE_RUNNING.any { it in detail } -> StartRefusal.NodeRunning
            KASSA_RUNNING in detail -> StartRefusal.KassaRunning
            KASSA_TAKEN in detail -> StartRefusal.BothDatabases
            else -> StartRefusal.NodeDataUnfit
        }
        return StartProblem(refusal, detail)
    }

    /** Так ядро говорит, что узел ещё работает над своей базой. */
    private val NODE_RUNNING = listOf("stop it before the import", "stop the node")

    /** Так ядро говорит, что каталог кассы занят другим её окном. */
    private const val KASSA_RUNNING = "is used by a running cash register"

    /** Так ядро говорит, что у кассы уже своя база. */
    private const val KASSA_TAKEN = "the import goes only into an empty one"
}
