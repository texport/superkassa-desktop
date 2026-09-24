package kz.mybrain.superkassa.integrations.ncalayer

/**
 * Почему подпись не получена.
 *
 * Случаи различаются тем, что делать владельцу: запустить NCALayer,
 * подождать и повторить или ничего — он сам закрыл окно. Прежде всё это
 * приходило одним отказом связи, и владелец читал «Запустите NCALayer»
 * про работающий.
 */
enum class NcaReason {
    /** NCALayer не ответил на рукопожатие: его нет или он не запущен. */
    Unreachable,

    /** Запрос NCALayer принял, а ответа не дал: окна владелец не видел. */
    Silent,

    /** Окно подписи было, и владелец закрыл его. */
    WindowClosed,

    /** NCALayer ответил отказом; его слова — в [NcaRefusal.detail]. */
    Declined
}

/**
 * Отказ подписи.
 *
 * Слов для владельца здесь нет: их подбирает приложение на его языке
 * по [reason]. [detail] — слова самого NCALayer или имя случая; на экран
 * они идут только как подробность.
 *
 * @property reason что случилось.
 * @property detail код и сообщение NCALayer, а для прочих случаев — имя случая.
 * @property askPreviousModule стоит ли просить ту же подпись прежним модулем
 *   `commonUtils`: так отвечают выпуски NCALayer, не знающие `basics`. Нельзя,
 *   когда окно владельцу уже показали, — повтор откроет его второй раз.
 */
class NcaRefusal(
    val reason: NcaReason,
    val detail: String,
    cause: Throwable? = null,
    val askPreviousModule: Boolean = false
) : Exception(detail, cause) {

    /**
     * Владелец сам отказался подписывать.
     *
     * NCALayer говорит об этом своими словами — `action.canceled`, —
     * и повторять нечего ни тем модулем, ни другим.
     */
    val cancelled: Boolean
        get() = CANCEL_WORDS.any { detail.contains(it, ignoreCase = true) }

    private companion object {
        val CANCEL_WORDS = listOf("cancel", "отмен", "abort")
    }
}

/**
 * NCALayer не отвечает: рукопожатия нет или соединение не поднялось.
 *
 * Причина сохраняется целиком: у отказов защищённого соединения сообщение
 * бывает пустым, и имя класса в журнале — единственная зацепка.
 */
internal fun ncaUnreachable(failure: Throwable? = null) =
    NcaRefusal(NcaReason.Unreachable, "no handshake", failure)

/** Запрос ушёл, а ответа нет; прежний модуль спросить стоит. */
internal fun ncaSilent(failure: Throwable? = null) =
    NcaRefusal(NcaReason.Silent, "no answer", failure, askPreviousModule = true)

/** Владелец закрыл окно подписи: NCALayer рвёт соединение молча. */
internal fun ncaWindowClosed(failure: Throwable? = null) =
    NcaRefusal(NcaReason.WindowClosed, "window closed", failure)
