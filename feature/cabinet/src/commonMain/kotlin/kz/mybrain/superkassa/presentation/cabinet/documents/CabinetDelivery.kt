package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kotlin.time.Instant

/**
 * Есть ли у документа кабинета печатная форма.
 *
 * Отвергнутый БФД документ фискальным не стал: его нет ни в БФД,
 * ни в отчётности. Печатная форма при этом выглядит как настоящий чек —
 * с номером, признаком и QR-кодом, — и покупатель принимает её
 * за подтверждение покупки.
 *
 * Журнал кассы так и решает про свои документы; кабинет заполняет
 * ту же таблицу, и мера у обоих одна. Чек, пробитый без связи, сюда
 * не попадает: он фискальный, просто ещё не доставлен.
 */
internal fun drawable(delivery: JournalDelivery?): Boolean = delivery != JournalDelivery.Refused

/**
 * Состояние доставки кабинета словами журнала.
 *
 * Кабинет отдаёт коды своего сервера — `ONLINE_OK`, `DELIVERY_ERROR`, —
 * и узел называет то же самое иначе. Обоим состояние приводится к одному
 * перечислению, иначе отбор «отклонённые» на двух экранах находил бы
 * разное.
 */
internal fun cabinetDelivery(code: String?): JournalDelivery? = when {
    code.isNullOrBlank() -> null
    REFUSED.any { code.contains(it, ignoreCase = true) } -> JournalDelivery.Refused
    // `ACCEPTED` — слово ближнего плеча: сервис приёма документ принял.
    // Без него состояние съезжало в «ждёт отправки», хотя ждать уже нечего.
    code.contains("OK", ignoreCase = true) ||
        code.equals("DELIVERED", ignoreCase = true) ||
        code.equals("ACCEPTED", ignoreCase = true) -> JournalDelivery.Delivered

    else -> JournalDelivery.Queued
}

/**
 * Состояние документа в кабинете по двум плечам передачи.
 *
 * Дальнее плечо — доставка в органы госдоходов, ближнее — приём
 * сервисом приёма. Пока службы передачи в контуре нет, дальнее пусто
 * у каждого документа, и столбец состояния выглядел сломанным: пустая
 * клетка на всю страницу. Ближнее известно всегда, и оно отвечает
 * на вопрос владельца «ушло ли из кассы».
 *
 * @param delivery состояние доставки в КГД, как его отдаёт кабинет.
 * @param send состояние приёма сервисом приёма.
 */
internal fun cabinetState(delivery: String?, send: String?): JournalDelivery? =
    cabinetDelivery(delivery) ?: cabinetDelivery(send)

/**
 * Момент документа числом.
 *
 * Кабинет отдаёт время строкой ISO-8601 в UTC; журнал сортирует строки
 * по числу — по тексту «2026-09-07T19:42» и «2026-10-01T08:00» сравнились
 * бы верно, а «07.09.2026» и «01.10.2026» из показа — уже нет.
 */
internal fun cabinetMillis(iso: String?): Long? {
    val value = iso?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()
}

/** Хвосты кодов, означающих отказ доставки. */
private val REFUSED = listOf("FAIL", "ERROR", "REJECT")
