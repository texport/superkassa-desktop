package kz.mybrain.superkassa.domain.cabinet.port

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Кто подписывает ключом владельца.
 *
 * Порт, а не NCALayer: ключ и пароль к нему остаются у владельца, а экран
 * знает только, что подпись просят и что её могут не дать. Подписью входят
 * в кабинет и подают заявления в ИСНА. Чем подписывать — NCALayer, eGov
 * mobile или файлом ключа — выбирает владелец через [Signing].
 */
interface Signer {

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * @param payload то, что подписывается, в base64 — как его выдал кабинет.
     * @throws EdsRefusal если подписывающий не отвечает, молчит или подписи не дал.
     */
    suspend fun sign(payload: String): String

    companion object {
        /**
         * Владелец закрыл окно подписи.
         *
         * Здесь стоит код, а не готовая строка: слова подбирает показ
         * на языке владельца, а по-русски посреди кода их читал бы
         * и казах, и англичанин.
         */
        const val WINDOW_CLOSED: String = "WINDOW_CLOSED"

        /** Подписывающий не ответил на рукопожатие: его нет или он не работает. */
        const val NO_HANDSHAKE: String = "NO_HANDSHAKE"

        /** Запрос подписывающий принял, а подписи не вернул: окна владелец не видел. */
        const val NO_ANSWER: String = "NO_ANSWER"

        /**
         * Владелец отменил подпись у кассы: закрыл выбор файла, окно пароля
         * или ожидание eGov mobile. Код читается как отказ владельца
         * ([kz.mybrain.superkassa.domain.cabinet.model.cancelledBySigner]).
         */
        const val CANCELLED: String = "CANCELLED"

        /** Служба, через которую eGov mobile получает и отдаёт подпись, не отвечает. */
        const val EGOV_UNREACHABLE: String = "EGOV_UNREACHABLE"

        /** Срок подписи в eGov mobile вышел, а подписи нет. */
        const val EGOV_EXPIRED: String = "EGOV_EXPIRED"

        /**
         * Сколько ждать подписи.
         *
         * Длинное намеренно: столько владелец выбирает сертификат
         * и вводит пароль. Экран показывает этот срок и даёт его
         * прервать — три минуты неподвижного экрана владелец читает
         * как зависшее приложение, и правильно читает.
         */
        val SIGN_WINDOW: Duration = 3.minutes
    }
}
