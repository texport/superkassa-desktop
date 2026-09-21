package kz.mybrain.superkassa.desktop.eds

/** Почему подпись не получена. */
enum class EdsProblem { Unreachable, Declined }

/** Отказ подписи, доведённый до экрана словами владельца. */
class EdsRefusal(
    val problem: EdsProblem,
    val detail: String,
    cause: Throwable? = null,
    /**
     * Отказ случился до того, как запрос ушёл в NCALayer.
     *
     * Тогда окна подписи он не открывал и о запросе не знает — повтор
     * владелец не заметит. После отправки повторять нельзя: окно
     * откроется второй раз.
     */
    val beforeRequest: Boolean = false
) : Exception(detail, cause) {

    /**
     * Владелец сам закрыл окно подписи.
     *
     * Повторять запрос другим модулем в этом случае значит открыть окно
     * второй раз подряд там, где человек только что отказался.
     */
    val cancelled: Boolean
        get() = CANCEL_WORDS.any { detail.contains(it, ignoreCase = true) }

    private companion object {
        val CANCEL_WORDS = listOf("cancel", "отмен", "abort")
    }
}

/**
 * Отказ связи с NCALayer.
 *
 * В подробностях стоит имя исключения: у отказов защищённого соединения
 * сообщение бывает пустым, и в журнале оставалось «подпись не получена:
 * Unreachable» — разбирать нечем, а разбирают такое по журналу с чужой
 * машины.
 *
 * @param sent ушёл ли запрос: от этого зависит, можно ли повторить.
 */
internal fun ncaUnreachable(failure: Throwable, sent: Boolean): EdsRefusal = EdsRefusal(
    problem = EdsProblem.Unreachable,
    detail = listOfNotNull(failure::class.simpleName, failure.message?.takeIf { it.isNotBlank() })
        .joinToString(": "),
    cause = failure,
    beforeRequest = !sent
)
