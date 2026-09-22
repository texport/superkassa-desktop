package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Возврат, история и очередь».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Названий типов документов и состояний доставки здесь нет намеренно: они
 * приходят из справочников узла, и собственный перевод рано или поздно
 * разошёлся бы с тем, что напечатано на чеке.
 */
data class JournalTexts(
    val returns: ReturnJournalTexts,
    val history: HistoryJournalTexts,
    val shifts: ShiftJournalTexts,
    val queue: QueueJournalTexts
)

/** Экран возврата: выбор чека-основания и сумма возврата. */
data class ReturnJournalTexts(
    val itemsToReturn: String,
    val basis: String,
    val basisHint: String,
    val basisColumn: String,
    val chooseBasis: String,
    val chooseBasisHint: String,
    val noBasisHint: String,
    val shiftClosedHint: String,
    val receiptTotal: String,
    val fiscalSign: String,
    val noSaleBasis: String,
    val noBuyBasis: String,
    val shiftClosed: String,
    val amount: String,
    val wholeReceipt: String,
    val partialHint: String,
    val amountInvalid: String,
    val amountTooLarge: String,
    val amountEmpty: String
)

/**
 * Журнал документов: срок, поиск, отбор и порядок строк.
 *
 * Набор один на два экрана — журнал кассы и документы кассы в кабинете:
 * ищут и отбирают в них одно и то же, и вторая копия надписей разошлась бы
 * с первой на первой же правке. Слова о сроке, поиске и порядке не говорят,
 * откуда пришли строки, — поэтому подходят и узлу, и кабинету.
 */
data class HistoryJournalTexts(
    val byPeriod: String,
    val byShift: String,
    val day: String,
    val today: String,
    val earlierDay: String,
    val laterDay: String,
    val documentType: String,
    val allTypes: String,
    val emptyDay: String,
    val emptyDayHint: String,
    val emptyForFilter: String,
    val emptyForFilterHint: String,
    val colTime: String,
    val colType: String,
    val colNumber: String,
    val colAmount: String,
    val colFiscalSign: String,
    val colShift: String,
    val shown: String,
    val showMore: String,
    val allShown: String,
    val search: String,
    val searchHint: String,
    val clearSearch: String,
    val sort: String,
    val sortTime: String,
    val sortAmount: String,
    val sortNumber: String,
    val ascending: String,
    val descending: String,
    val period: String,
    val spanWeek: String,
    val spanMonth: String,
    val spanAll: String,
    val earlierSpan: String,
    val laterSpan: String,
    val deliveryState: String,
    val allStates: String,
    val allShifts: String,
    val registerDocuments: String,
    val registerDocumentsHint: String,
    val openDocuments: String,
    val backToRegister: String
)

/** Прошлые смены и печать Z-отчёта закрытой смены. */
data class ShiftJournalTexts(
    val showMore: String,
    val allShown: String,
    val title: String,
    val load: String,
    val hint: String,
    val none: String,
    val noneHint: String,
    val number: String,
    val opened: String,
    val closed: String,
    val stillOpen: String,
    val zReport: String,
    val documents: String,
    val emptyDocuments: String,
    val emptyDocumentsHint: String,
    val back: String
)

/** Очередь отложенной отправки: состояния, причина неудачи и повтор. */
data class QueueJournalTexts(
    val task: String,
    val sentSection: String,

    /**
     * Задачи, отправки которых не будет.
     *
     * Своя строка, а не общая с отправленными: отвергнутая задача
     * стояла под заголовком «Уже отправлено» с плашкой «Не будет
     * отправлен» — заголовок спорил со строкой под ним, а счёт
     * отправленных включал то, что не ушло.
     */
    val rejectedSection: String,
    val sending: String,
    val retrying: String,
    val rejectedForGood: String,
    val nextAttempt: String,
    val lastFailure: String,
    val retryHint: String,
    val retryNeedsProgramming: String,
    val nothingFailed: String,
    /** Повторять нечего, но отвергнутое на экране есть: строка обязана это признать. */
    val nothingToRetryButRejected: String,
    val emptyHint: String
)

/** Надписи области на выбранном языке. */
fun journalTexts(language: Language): JournalTexts = when (language) {
    Language.Kk -> journalTextsKk
    Language.Ru -> journalTextsRu
    Language.En -> journalTextsEn
}
