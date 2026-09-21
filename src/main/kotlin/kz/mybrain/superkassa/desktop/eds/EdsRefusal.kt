package kz.mybrain.superkassa.desktop.eds

/**
 * Почему подпись не получена.
 *
 * Различие тут одно и важное: [Unreachable] — NCALayer не отвечает вовсе,
 * и владельцу надо его запустить; [Declined] — NCALayer на связи, а подписи
 * нет. Второе приходило под первым именем, и владелец читал «Запустите
 * NCALayer» про работающий NCALayer. Что именно случилось во втором
 * случае — в [EdsRefusal.detail].
 */
enum class EdsProblem { Unreachable, Declined }

/** Отказ подписи, доведённый до экрана словами владельца. */
class EdsRefusal(
    val problem: EdsProblem,
    val detail: String,
    cause: Throwable? = null,
    /**
     * Стоит ли просить ту же подпись прежним модулем.
     *
     * Старые выпуски NCALayer модуля `basics` не знают: на запрос к нему
     * они молчат или закрывают соединение, не показав окна. Это и значит
     * «спроси прежним» — а не «повтори то же самое».
     *
     * Нельзя, когда окно владельцу уже показали: повтор откроет его
     * второй раз подряд там, где человек только что отказался.
     */
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
 * бывает пустым, и разбирают такое по журналу с чужой машины — имя класса
 * там единственная зацепка. На экран оно не идёт: владельцу нужно
 * «запустите NCALayer», а не `SSLException`.
 */
internal fun ncaUnreachable(failure: Throwable? = null): EdsRefusal = EdsRefusal(
    problem = EdsProblem.Unreachable,
    detail = NcaLayer.NO_HANDSHAKE,
    cause = failure
)

/**
 * Запрос ушёл, а ответа нет.
 *
 * Отдельный случай от недоступного NCALayer: соединение поднялось,
 * запрос он принял — значит, запускать его не надо, и предлагать это
 * владельцу нельзя. Зато прежний модуль спросить стоит: так выглядит
 * выпуск NCALayer, который не знает `basics`.
 */
internal fun ncaSilent(failure: Throwable? = null): EdsRefusal = EdsRefusal(
    problem = EdsProblem.Declined,
    detail = NcaLayer.NO_ANSWER,
    cause = failure,
    askPreviousModule = true
)

/**
 * Владелец закрыл окно подписи: NCALayer рвёт соединение молча.
 *
 * Прежде это доходило до экрана именем класса — владелец видел «Кабинет
 * не отвечает» там, где сам и закрыл окно.
 */
internal fun ncaWindowClosed(failure: Throwable? = null): EdsRefusal = EdsRefusal(
    problem = EdsProblem.Declined,
    detail = NcaLayer.WINDOW_CLOSED,
    cause = failure
)
