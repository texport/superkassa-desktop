package kz.mybrain.superkassa.strings.api.journal

/**
 * Надписи окна «Доставка чека покупателю» в журнале документов.
 *
 * Отдельно от надписей журнала: окно своё, и поля его не должны
 * теряться среди столбцов и отбора.
 *
 * Названия каналов — свои, а не коды ядра: `WHATSAPP` и `EMAIL` на экране
 * кассира не пишутся. Незнакомый канал называется словами [otherChannel],
 * а не кодом.
 */
data class DeliveryTexts(
    val title: String,
    val resend: String,
    val close: String,
    val pending: String,
    val delivered: String,
    val failed: String,
    val attempts: String,
    val nextAttempt: String,

    /** Касса не ответила, что с доставкой: это не «доставки нет». */
    val unread: String,
    val notOrdered: String,
    val notOrderedHint: String,

    /** Чек пробит без связи: задачи доставки ставятся, когда БФД его примет. */
    val awaitingBfd: String,
    val awaitingBfdHint: String,
    val sms: String,
    val telegram: String,
    val whatsapp: String,
    val email: String,
    val printer: String,
    val otherChannel: String,
    val link: String,
    val image: String,
    val page: String
)
