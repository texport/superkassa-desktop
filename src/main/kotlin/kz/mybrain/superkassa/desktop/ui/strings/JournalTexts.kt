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

    /**
     * Весь день показан: чеков-оснований больше нет.
     *
     * Своя строка, а не журнальная «Показан весь срок»: журнал листают
     * сроком, а основание ищут за один день, и о сроке под списком чеков
     * дня говорить нечего.
     */
    val allBasesShown: String,
    val chooseBasis: String,
    val chooseBasisHint: String,
    val noBasisHint: String,

    /**
     * Чеки дня прочитать не удалось.
     *
     * Отдельно от «оснований нет»: узел отказал или не ответил, и о чеках
     * покупателя он не сказал ничего. Кассир при покупателе с чеком в руках
     * читал молчание узла как отказ в возврате.
     */
    val basisUnread: String,
    val basisUnreadHint: String,
    val shiftClosedHint: String,
    val receiptTotal: String,
    val fiscalSign: String,
    val noSaleBasis: String,
    val noBuyBasis: String,
    val shiftClosed: String,

    /**
     * Касса заблокирована — в том числе снята с учёта.
     *
     * Узел фискальных команд такой кассе не проводит, а смена у неё
     * может оставаться открытой: без своего состояния экран предлагал
     * кассиру кнопку, на которую узел отвечает KKM_BLOCKED.
     */
    val kkmBlocked: String,
    val kkmBlockedHint: String,
    val amount: String,
    val wholeReceipt: String,
    val partialHint: String,
    val amountInvalid: String,
    val amountTooLarge: String,
    val amountEmpty: String,

    /**
     * Сумма возврата набрана не по отметкам.
     *
     * Отмеченные позиции уходят строками чека только тогда, когда сумма
     * возврата — это в точности их сумма. Иначе чек описывал бы строками
     * одну сумму, а оплатой другую, и ОФД принять его не может.
     */
    val itemsIgnored: String,

    /**
     * В ящике меньше денег, чем отдают покупателю наличными.
     *
     * Возврат продажи берёт деньги из того же ящика, из которого их
     * изымают, и о нехватке кассир должен узнать до того, как назовёт
     * сумму покупателю, а не из отказа узла после.
     */
    val drawerShort: String
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

    /** Документы срока прочитать не удалось: узел отказал или не ответил. */
    val unread: String,
    val unreadHint: String,
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

    /** Смены прочитать не удалось: узел отказал или не ответил. */
    val unread: String,
    val unreadHint: String,
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
    val emptyHint: String,
    /**
     * Пустая очередь заблокированной кассы.
     *
     * «Касса работает на связи с БФД» над кассой, которая встала, —
     * неправда в ту сторону, в какую ошибаться нельзя: кассир уходит
     * с экрана уверенный, что всё в порядке.
     */
    val emptyBlockedHint: String
)

/** Надписи области на выбранном языке. */
fun journalTexts(language: Language): JournalTexts = when (language) {
    Language.Kk -> journalTextsKk
    Language.Ru -> journalTextsRu
    Language.En -> journalTextsEn
}
