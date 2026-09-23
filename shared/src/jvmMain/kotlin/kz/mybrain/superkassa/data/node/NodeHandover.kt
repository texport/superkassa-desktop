package kz.mybrain.superkassa.data.node

import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import java.io.File

/**
 * Передача данных узла кассе процесса — при запуске, до кассы и до узла.
 *
 * Перенос делается один раз: кассы, смены, документы, очередь и кассиры
 * узла ложатся в базу кассы процесса, и узел после этого не запускается —
 * на его прежней базе касса выдала бы документы с уже занятыми номерами.
 *
 * Пока часть разделов говорит с узлом, перенос выключен: без узла они
 * не работают. Включается явно — переменной [VARIABLE] или свойством
 * [PROPERTY] со значением `true`.
 */
object NodeHandover {

    const val VARIABLE = "SUPERKASSA_IMPORT_NODE"
    const val PROPERTY = "superkassa.importNode"

    /** Включён ли перенос в этом запуске. */
    fun enabled(
        variable: String? = System.getenv(VARIABLE),
        property: String? = System.getProperty(PROPERTY)
    ): Boolean =
        (variable?.takeIf { it.isNotBlank() } ?: property).toBoolean()

    /**
     * Переносит данные узла, если перенос включён.
     *
     * @param nodeHome рабочее место узла: там его настройки и база.
     * @param kassaDir каталог кассы процесса — тот же, на котором она поднимется.
     * @param nodeAddress адрес узла: ответивший узел ещё пишет, и перенос не начинается.
     * @return что стало с данными узла; `null` — перенос выключен.
     * @throws io.github.texport.superkassa.importnode.api.NodeImportException перенос не сделан,
     *   каталог кассы не тронут, и кассу поднимать нельзя.
     */
    fun run(nodeHome: File, kassaDir: File, nodeAddress: String?, enabled: Boolean = enabled()): NodeImportResult? =
        if (enabled) importNodeData(nodeHome.path, kassaDir.path, nodeAddress) else null

    /** Данные узла уже у кассы: узел больше не запускается. */
    fun retired(result: NodeImportResult?): Boolean =
        result is NodeImportResult.Imported || result == NodeImportResult.AlreadyImported
}
