package kz.mybrain.superkassa.desktop.eds

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Подпись ЭЦП через NCALayer.
 *
 * Ключ и пароль к нему остаются у владельца: приложение отправляет
 * в NCALayer то, что нужно подписать, а окно выбора сертификата и ввода
 * пароля показывает сам NCALayer. Ни ключ, ни пароль сюда не приходят
 * и нигде не хранятся.
 *
 * NCALayer слушает петлю по защищённому соединению с самоподписанным
 * сертификатом на `127.0.0.1`. Проверять его нечем и незачем: собеседник
 * — процесс на этой же машине, а не узел в сети. Доверие ограничено
 * этим соединением и не распространяется на обмен с кабинетом и узлом.
 *
 * @param signWindow сколько ждать подписи. Столько же экран отсчитывает
 *   владельцу: ожидание с видимым сроком он может переждать, а неподвижный
 *   экран — нет.
 */
class NcaLayer(
    address: String = DEFAULT_ADDRESS,
    signWindow: Duration = SIGN_WINDOW
) {

    private val exchange = NcaExchange(address, signWindow)

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * Запасной путь к прежнему модулю идёт и при молчании нового:
     * выпуски NCALayer, которые не знают `basics`, не отказывают — они
     * молчат или закрывают соединение, не показав окна. Прежде запасной
     * путь ждал явного отказа и при молчании не срабатывал, хотя заведён
     * ровно для этого.
     *
     * Отмену владельца метод не перехватывает: по ней ничего
     * не повторяется и ничего не показывается.
     *
     * @param base64Content то, что подписывается, в base64 — как его выдал кабинет.
     * @return CMS-подпись в base64.
     * @throws EdsRefusal если NCALayer не отвечает, молчит или подписи не дал.
     */
    suspend fun signCms(base64Content: String): String {
        val refused = try {
            return ncaSignatureOf(exchange.ask(ncaSignRequest(base64Content)))
        } catch (refusal: EdsRefusal) {
            refusal
        }
        if (!refused.askPreviousModule || refused.cancelled) throw refused
        ncaJournal("модуль basics не подписал (${refused.detail}), просим прежний")
        return ncaLegacySignatureOf(exchange.ask(ncaLegacyRequest(base64Content)))
    }

    companion object {
        /** Где NCALayer слушает на этой машине. */
        const val DEFAULT_ADDRESS: String = "wss://127.0.0.1:13579"

        /**
         * Владелец закрыл окно подписи.
         *
         * Здесь стоит код, а не готовая строка: слова подбирает показ
         * на языке владельца, а по-русски посреди кода их читал бы
         * и казах, и англичанин.
         */
        const val WINDOW_CLOSED: String = "WINDOW_CLOSED"

        /** NCALayer не ответил на рукопожатие: его нет или он не работает. */
        const val NO_HANDSHAKE: String = "NO_HANDSHAKE"

        /** Запрос NCALayer принял, а подписи не вернул: окна владелец не видел. */
        const val NO_ANSWER: String = "NO_ANSWER"

        /**
         * Сколько ждать подписи после того, как NCALayer ответил.
         *
         * Длинное намеренно: столько владелец выбирает сертификат
         * и вводит пароль. Экран показывает этот срок и даёт его
         * прервать — три минуты неподвижного экрана владелец читает
         * как зависшее приложение, и правильно читает.
         */
        val SIGN_WINDOW: Duration = 3.minutes
    }
}
