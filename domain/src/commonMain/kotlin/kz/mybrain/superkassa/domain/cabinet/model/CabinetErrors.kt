package kz.mybrain.superkassa.domain.cabinet.model

/**
 * Отказ кабинета, доведённый до приложения целиком.
 *
 * Кабинет отвечает кодом и сообщением на одном языке — в отличие от узла,
 * который присылает три. Код сохраняется: по нему видно, истекла ли
 * сессия (`UNAUTHORIZED`) или заявление отвергнуто ИСНА.
 */
class CabinetRefusal(
    val code: String,
    val text: String,
    val httpStatus: Int,
    cause: Throwable? = null
) : Exception("$code: $text", cause)

/**
 * Кабинет ответил, а прочитать ответ нечем.
 *
 * Так выглядит разошедшийся договор: поле сменило имя или тип, ответ
 * пришёл успешным кодом и не разобрался. От недоступности службы это
 * отличается всем, и сводить их к одному нельзя: владелец шёл проверять
 * сеть и доступ к кабинету, который отвечает и работает.
 */
class CabinetUnreadable(path: String, cause: Throwable) : Exception("$path: ${cause.message}", cause)

/**
 * Доступ к кабинету кончился: истёк срок или владелец не входил.
 *
 * Не отказ, а конец сеанса: владельца возвращает ко входу, а не оставляет
 * с пустым списком без объяснения.
 */
class CabinetExpired(cause: Throwable? = null) : Exception("cabinet access expired", cause)

/**
 * Кабинет не ответил: нет соединения или истекло ожидание.
 *
 * @property reason имя помехи сети — зацепка для журнала; владельцу оно
 *   ничего не объясняет.
 */
class CabinetUnreachable(val reason: String, cause: Throwable? = null) :
    Exception("cabinet unreachable: $reason", cause)

/**
 * Почему подпись не получена.
 *
 * Различие тут одно и важное: [EdsProblem.Unreachable] — NCALayer не отвечает вовсе,
 * и владельцу надо его запустить; [EdsProblem.Declined] — NCALayer на связи, а подписи
 * нет. Второе приходило под первым именем, и владелец читал «Запустите
 * NCALayer» про работающий NCALayer. Что именно случилось во втором
 * случае — в [EdsRefusal.detail].
 */
enum class EdsProblem { Unreachable, Declined }

/** Отказ подписи, доведённый до экрана словами владельца. */
class EdsRefusal(
    val problem: EdsProblem,
    val detail: String,
    cause: Throwable? = null
) : Exception(detail, cause) {

    /**
     * Владелец сам отказался подписывать.
     *
     * NCALayer говорит об этом своими словами — `action.canceled`, —
     * и повторять нечего.
     */
    val cancelled: Boolean
        get() = cancelledBySigner(detail)
}

/**
 * Отказ владельца — по словам самого NCALayer.
 *
 * Слова здесь одни на приложение: по ним отказ отличают и в разборе
 * помехи, и в сообщении владельцу, и разойтись они не должны.
 */
fun cancelledBySigner(detail: String): Boolean =
    CANCEL_WORDS.any { detail.contains(it, ignoreCase = true) }

private val CANCEL_WORDS = listOf("cancel", "отмен", "abort")
