package kz.mybrain.superkassa.strings.api.journal

/**
 * Журнал документов: срок, поиск, отбор и порядок строк.
 *
 * Набор один на два экрана — журнал кассы и документы кассы в кабинете:
 * ищут и отбирают в них одно и то же, и вторая копия надписей разошлась бы
 * с первой на первой же правке. Слова о сроке, поиске и порядке не говорят,
 * откуда пришли строки, — поэтому подходят и кассе, и кабинету.
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

    /** Документы срока прочитать не удалось: касса отказала или не ответила. */
    val unread: String,
    val unreadHint: String,
    val colTime: String,
    val colType: String,
    val colNumber: String,
    val colAmount: String,
    val colFiscalSign: String,
    val colShift: String,
    val shown: String,

    /**
     * Сколько строк показано из прочитанного, когда срок прочитан не весь.
     *
     * «Показано: 200 / 200» под кнопкой «Показать ещё» читается как весь
     * срок: владелец видел два одинаковых числа и уходил уверенный, что
     * за день пробито двести чеков. Второе число — это прочитанное
     * до сих пор, и строка обязана назвать его своим именем.
     */
    val shownOfRead: String,
    val showMore: String,
    val allShown: String,
    val search: String,
    val searchHint: String,
    val clearSearch: String,
    val sortTime: String,
    val sortAmount: String,
    val sortNumber: String,
    val ascending: String,
    val descending: String,
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
