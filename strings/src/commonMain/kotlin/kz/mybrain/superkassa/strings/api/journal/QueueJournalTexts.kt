package kz.mybrain.superkassa.strings.api.journal

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
    val retryNeedsClosedShift: String,
    val nothingFailed: String,
    /** Повторять нечего, но отвергнутое на экране есть: строка обязана это признать. */
    val nothingToRetryButRejected: String,
    val emptyHint: String,

    /**
     * Очередь прочитать не удалось.
     *
     * Отдельно от пустой очереди: касса отказала или не ответила, и о ждущих
     * документах он не сказал ничего. Владелец читал его молчание как
     * «всё доставлено» — ошибка в ту сторону, в какую ошибаться нельзя.
     */
    val unread: String,
    val unreadHint: String,
    /**
     * Пустая очередь заблокированной кассы.
     *
     * «Касса работает на связи с БФД» над кассой, которая встала, —
     * неправда в ту сторону, в какую ошибаться нельзя: кассир уходит
     * с экрана уверенный, что всё в порядке.
     */
    val emptyBlockedHint: String
)
