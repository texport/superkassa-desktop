package kz.mybrain.superkassa.strings.api.journal

/** Прошлые смены и печать Z-отчёта закрытой смены. */
data class ShiftJournalTexts(
    val showMore: String,
    val allShown: String,
    val title: String,
    val load: String,
    val hint: String,
    val none: String,
    val noneHint: String,

    /** Смены прочитать не удалось: касса отказала или не ответила. */
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

    /**
     * Документы смены прочитать не удалось.
     *
     * Отдельно от «в смене документов нет»: касса отказала или не ответила,
     * и о чеках смены он не сказал ничего. Кассир, пришедший за чеком
     * позавчерашней смены, читал это молчание как пустую смену.
     */
    val documentsUnread: String,
    val documentsUnreadHint: String,
    val back: String
)
