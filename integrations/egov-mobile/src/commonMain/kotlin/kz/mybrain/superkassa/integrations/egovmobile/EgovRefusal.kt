package kz.mybrain.superkassa.integrations.egovmobile

/** Почему подпись eGov mobile не получена. */
enum class EgovReason {
    /** Посредник не отвечает: нет сети или он недоступен. */
    Unreachable,

    /** Посредник ответил отказом. */
    Refused,

    /** Владелец отказался подписывать в eGov mobile. */
    Cancelled,

    /** Срок подписи вышел. */
    Expired
}

/**
 * Подпись eGov mobile не получена.
 *
 * @property detail слова посредника или имя помехи — для журнала и поддержки;
 *   ни данных, ни подписи в них нет.
 */
class EgovRefusal(val reason: EgovReason, val detail: String = "", cause: Throwable? = null) :
    Exception("egov mobile: $reason $detail".trim(), cause)
