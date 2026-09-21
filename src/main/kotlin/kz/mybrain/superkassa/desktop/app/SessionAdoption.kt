package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.CounterRecord
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.NodeVatRate
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.history.Shift

/**
 * Приём прочитанного у узла в состояние рабочего места.
 *
 * Отдельный предмет от самого сеанса: сеанс отвечает на вопросы экранов,
 * а здесь собрано всё, чем на эти вопросы меняется ответ. Каждая из этих
 * строк — конец одного обращения к узлу из [SessionRefresh], и держать их
 * вместе значит видеть в одном месте, что именно узел вправе поменять.
 */

/** Принимает список касс от узла: ответ узла означает, что он на связи. */
internal fun Session.adoptKkms(loaded: List<Kkm>) {
    kkms.clear()
    kkms.addAll(loaded)
    settings.adoptNames(loaded)
    calls.answered()
    selected = selected?.let { was -> loaded.firstOrNull { it.kkmId == was.kkmId } ?: was }
}

/**
 * Принимает состояние одной кассы — той, что выбрана.
 *
 * Отдельно от списка: перечитывать весь парк после каждого чека дорого,
 * а состояние выбранной кассы меняется у неё же. Узел переводит кассу
 * в BLOCKED, узнав из ответа ОФД, что она снята с учёта, — и шапка
 * обязана это показать, не дожидаясь перезахода кассира.
 */
internal fun Session.adoptKkm(loaded: Kkm) {
    kkms.replaceAll { if (it.kkmId == loaded.kkmId) loaded else it }
    settings.adoptNames(listOf(loaded))
    calls.answered()
    if (selected?.kkmId == loaded.kkmId) selected = loaded
}

internal fun Session.adoptShift(shift: Shift?) = board.adoptShift(shift)

internal fun Session.forgetShift() = board.forgetShift()

internal fun Session.adoptDocuments(loaded: List<Document>) = board.adoptDocuments(loaded)

internal fun Session.adoptQueue(loaded: List<QueueTask>) = board.adoptQueue(loaded)

internal fun Session.adoptCounters(loaded: List<CounterRecord>) = board.adoptCounters(loaded)

internal fun Session.adoptUnits(loaded: List<UnitOfMeasurement>) = reference.adoptUnits(loaded)

internal fun Session.adoptVatRates(loaded: List<NodeVatRate>) = reference.adoptVatRates(loaded)

/** Смена закрыта: состояние кассы, а не отказ, и строка отказа снимается. */
internal fun Session.noShift() {
    board.closed()
    lastMessage = null
}
