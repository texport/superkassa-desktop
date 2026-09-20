package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.counters
import kz.mybrain.superkassa.desktop.server.listKkms
import kz.mybrain.superkassa.desktop.server.queue
import kz.mybrain.superkassa.desktop.server.shiftDocuments
import kz.mybrain.superkassa.desktop.ui.history.lastShift

/**
 * Перечитывает список касс. Отсутствие узла — не ошибка кассира.
 *
 * Итог только что выполненного действия переживает перечитывание:
 * перечитывание — это не действие кассира, и стирать им ответ на то,
 * что кассир нажал, нельзя.
 */
suspend fun Session.refreshKkms(): Unit? {
    val previous = lastMessage
    loadMissingDictionaries()
    val result = guard<Unit>(texts.login.reload) { adoptKkms(client.listKkms()) }
    if (lastMessage == null) lastMessage = previous
    return result
}

/**
 * Перечитывает всё, что относится к выбранной кассе.
 *
 * Обновление идёт тремя обращениями, и каждое чистит сообщение за собой.
 * Первый отказ запоминается и возвращается в строку в конце: иначе
 * удачное третье обращение стирало бы отказ первого, и кассир не узнал
 * бы, что документы не перечитались.
 */
suspend fun Session.refreshSelected() {
    val kkm = selected ?: return
    // Итог только что выполненного действия переживает перечитывание:
    // иначе «чек пробит» стирается обновлением, которое само это
    // действие и вызвало, и кассир остаётся без подтверждения.
    // Отказ переживает его наравне с удачей: «очередь не отправлена»
    // кассиру нужнее, чем молчание.
    val done = lastMessage
    val problems = listOfNotNull(
        readShift(kkm.kkmId),
        readDocuments(kkm.kkmId),
        readQueue(kkm.kkmId),
        readCounters(kkm.kkmId)
    )
    // Ответ на нажатую кнопку сильнее беды перечитывания: кассир
    // спрашивал «что сделала кнопка», и подменять этот ответ отказом
    // фонового чтения значит не ответить вовсе.
    lastMessage = done ?: problems.firstOrNull()
}

/**
 * Состояние смены — со слов узла, а не по догадке.
 *
 * Прежде открытость смены выводилась из того, ответил ли узел на список
 * документов текущей смены. У кассы, снятой с учёта, он отвечает на него
 * KKM_BLOCKED, и главный экран писал «Смена закрыта» и предлагал
 * «Открыть смену» над сменой, которую узел держал открытой: нажатие
 * получало SHIFT_ALREADY_OPEN. Список смен узел отдаёт и по такой кассе.
 */
private suspend fun Session.readShift(kkmId: String): Message? {
    guard<Unit>(texts.dashboard.shift) { adoptShift(client.lastShift(kkmId, pin)) }
    val problem = lastMessage
    if (problem != null) forgetShift()
    return problem
}

/** Документы смены; закрытая смена — состояние кассы, а не отказ. */
private suspend fun Session.readDocuments(kkmId: String): Message? {
    // Документы спрашиваются у открытой смены: у закрытой их нет по
    // существу, а неизвестное состояние — повод молчать, а не гадать.
    if (shiftState != ShiftState.Open) return null
    guard<Unit>(texts.dashboard.shiftDocuments) {
        adoptDocuments(client.shiftDocuments(kkmId, pin))
    }
    val problem = lastMessage
    if (problem is Message.Refusal && problem.code == SHIFT_NOT_OPEN) {
        noShift()
        return null
    }
    return problem
}

/**
 * Очередь отправки: узел показывает её только администратору.
 *
 * Кассиру её не спрашиваем, иначе вход встречал бы его отказом
 * на пустом месте.
 */
private suspend fun Session.readQueue(kkmId: String): Message? {
    if (!isAdmin) {
        adoptQueue(emptyList())
        return null
    }
    guard<Unit>(texts.queue.title) { adoptQueue(client.queue(kkmId, pin)) }
    return lastMessage
}

/** Счётчики кассы: из них берутся наличные в ящике. */
private suspend fun Session.readCounters(kkmId: String): Message? {
    guard<Unit>(texts.dashboard.cashInDrawer) { adoptCounters(client.counters(kkmId, pin)) }
    return lastMessage
}
