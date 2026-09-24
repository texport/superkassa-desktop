package kz.mybrain.superkassa.domain.kassa.model.payment

import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse

/**
 * Виды оплаты для экрана: справочник кассы, а до его ответа — известные
 * приложению.
 *
 * Пустой список оставил бы кассира без выбора вида оплаты вовсе.
 */
fun paymentEntries(read: List<PaymentTypeResponse>): List<PaymentTypeResponse> = read.ifEmpty { KNOWN_PAYMENTS }

/**
 * Виды оплаты, которые касса сейчас не принимает.
 *
 * Берётся из справочника кассы, а не из зашитого списка: набор
 * непринимаемых видов меняется вместе с версией протокола, и своя копия
 * разошлась бы с кассой на первой же смене версии.
 */
internal fun unsupportedPayments(read: List<PaymentTypeResponse>): Set<String> =
    paymentEntries(read).filterNot { it.supported }.map { it.code }.toSet()

/**
 * Виды оплаты, которых нет в протоколе 2.0.4.
 *
 * Это запасной ответ на случай, когда справочник кассы ещё не прочитан:
 * сама касса объявляет допустимость каждого вида полем `supported`.
 * Прятать непринимаемый вид из списка нельзя — справочник его отдаёт, —
 * но выбор такого вида обязан объясниться словами до нажатия кнопки.
 */
internal val UNSUPPORTED_PAYMENTS: Set<String> = setOf("CREDIT", "TARE")

/**
 * Виды оплаты, когда справочник кассы ещё не прочитан.
 *
 * Названия для этих кодов у приложения свои — пустое название справочника
 * подменяется ими на экране. Допустимость до ответа кассы берётся из
 * последнего известного состояния протокола.
 */
private val KNOWN_PAYMENTS = listOf("CASH", "CARD", "ELECTRONIC", "MOBILE", "CREDIT", "TARE")
    .map { PaymentTypeResponse(it, TrilingualMessageResponse("", "", ""), supported = it !in UNSUPPORTED_PAYMENTS) }
