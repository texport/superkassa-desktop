package kz.mybrain.superkassa.desktop.eds

/** Почему подпись не получена. */
enum class EdsProblem { Unreachable, Declined }

/** Отказ подписи, доведённый до экрана словами владельца. */
class EdsRefusal(
    val problem: EdsProblem,
    val detail: String,
    cause: Throwable? = null
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
