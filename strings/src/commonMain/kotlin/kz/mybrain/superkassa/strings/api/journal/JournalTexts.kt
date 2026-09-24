package kz.mybrain.superkassa.strings.api.journal

/**
 * Надписи области «Возврат, история и очередь».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Названий типов документов и состояний доставки здесь нет намеренно: они
 * приходят из справочников кассы, и собственный перевод рано или поздно
 * разошёлся бы с тем, что напечатано на чеке.
 */
data class JournalTexts(
    val returns: ReturnJournalTexts,
    val history: HistoryJournalTexts,
    val shifts: ShiftJournalTexts,
    val queue: QueueJournalTexts,
    /** Доставка чека покупателю из журнала. */
    val delivery: DeliveryTexts,
    /** Отказы БФД словами кассира. */
    val ofdRefusal: OfdRefusalTexts
)
