package kz.mybrain.superkassa.desktop.ui.payment

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry

/**
 * Виды оплаты для экрана: справочник узла, а до его ответа — известные
 * приложению.
 *
 * Пустой список оставил бы кассира без выбора вида оплаты вовсе.
 */
fun paymentEntries(session: Session): List<DictionaryEntry> =
    session.dictionaries[Dictionary.PaymentTypes].orEmpty().ifEmpty { KNOWN_PAYMENTS }

/**
 * Виды оплаты, которые узел сейчас не принимает.
 *
 * Берётся из ответа узла, а не из зашитого списка: набор непринимаемых
 * видов меняется вместе с версией протокола, и своя копия разошлась бы
 * с узлом на первой же смене версии.
 */
fun unsupportedPayments(session: Session): Set<String> =
    paymentEntries(session).filterNot { it.supported }.map { it.code }.toSet()

/**
 * Виды оплаты, которых нет в протоколе 2.0.4.
 *
 * Это запасной ответ на случай, когда справочник узла ещё не прочитан:
 * сам узел объявляет допустимость каждого вида полем `supported`, и
 * работающее приложение читает её оттуда. Прятать непринимаемый вид
 * из списка нельзя — справочник узла его отдаёт, — но выбор такого вида
 * обязан объясниться словами до нажатия кнопки.
 */
val UNSUPPORTED_PAYMENTS: Set<String> = setOf("CREDIT", "TARE")

/**
 * Виды оплаты, когда справочник узла ещё не прочитан.
 *
 * Названия для этих кодов у приложения свои — те же, что и для присланных
 * узлом без названия. Допустимость до ответа узла берётся из последнего
 * известного состояния протокола.
 */
private val KNOWN_PAYMENTS = listOf("CASH", "CARD", "ELECTRONIC", "MOBILE", "CREDIT", "TARE")
    .map { DictionaryEntry(code = it, supported = it !in UNSUPPORTED_PAYMENTS) }

/**
 * Показывать ли пояснение о погасших видах оплаты.
 *
 * Пояснение относится к погасшим строкам списка, а не к выбранному виду.
 * Без этого условия касса писала «протокол 2.0.4 его не принимает» под
 * наличными — видом, который принимают все версии протокола, — и кассир
 * читал это как отказ принять деньги.
 *
 * @param note сама фраза; пустая — пояснения нет.
 * @param entries виды оплаты из справочника узла.
 */
fun unsupportedNoteVisible(note: String, entries: List<DictionaryEntry>): Boolean =
    note.isNotBlank() && entries.any { !it.supported }
