package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Где кабинет и как с ним обходиться.
 *
 * @property baseUrl адрес кабинета: он может стоять и на стенде, и у себя,
 *   поэтому задаётся настройкой рабочего места.
 * @property development личность разработчика: кабинет в режиме разработки
 *   берёт пользователя и компанию из заголовков `X-Debug-Iin`/`X-Debug-Bin`,
 *   а не из входа по ЭЦП. `null` — обычный вход; задаётся только явно.
 * @property connectWait сколько ждать соединения: недоступную службу видно сразу.
 * @property answerWait сколько ждать ответа. Самые долгие ответы — выпуск
 *   токена и подача заявления в КГД; полминуты покрывают их с запасом.
 * @property reconnectPause пауза перед второй попыткой соединения.
 * @property tokenAttempts сколько раз спрашивать токен, пока сервис приёма
 *   не подтвердит запись (`PENDING`).
 * @property tokenPause пауза между такими вопросами.
 */
data class CabinetSettings(
    val baseUrl: String = DEFAULT_URL,
    val development: DevelopmentIdentity? = null,
    val connectWait: Duration = 10.seconds,
    val answerWait: Duration = 30.seconds,
    val reconnectPause: Duration = 400.milliseconds,
    val tokenAttempts: Int = 10,
    val tokenPause: Duration = 1.seconds
) {
    /** Адрес кабинета по умолчанию. */
    companion object {
        /** Кабинет ECC. */
        const val DEFAULT_URL: String = "http://bfd-cabinet.ecc.kz"
    }
}

/**
 * Личность владельца в режиме разработки кабинета.
 *
 * Только для стенда и разработки: пока она задана, доступ по ЭЦП
 * не передаётся — его у такого сеанса нет.
 *
 * @property iin ИИН пользователя.
 * @property bin БИН компании.
 */
data class DevelopmentIdentity(val iin: String, val bin: String) {
    /** Имена заголовков режима разработки. */
    companion object {
        /** Заголовок с ИИН пользователя. */
        const val IIN_HEADER: String = "X-Debug-Iin"

        /** Заголовок с БИН компании. */
        const val BIN_HEADER: String = "X-Debug-Bin"
    }
}

/**
 * Кто подписывает ключом владельца.
 *
 * Модуль требует подпись, а не делает её: ключ и пароль остаются у владельца,
 * а подписывающий — NCALayer на настольной машине или иной на других —
 * выбирается приложением. Подписью входят в кабинет; заявления подписываются
 * тем же подписывающим между подготовкой заявления и его отправкой.
 */
fun interface CabinetSigner {

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * @param payload то, что подписывается, в base64 — как его выдал кабинет.
     * @throws Exception любой отказ подписывающего; модуль его не перехватывает.
     */
    suspend fun sign(payload: String): String
}

/**
 * Журнал обмена с кабинетом.
 *
 * В строку идут метод, путь без строки запроса, код ответа и длительность —
 * ни тел, ни доступа: журнал владелец пересылает в поддержку целиком.
 */
fun interface CabinetJournal {

    /** Записывает строку обмена; [failure] — помеха, если ответа не было. */
    fun record(line: String, failure: Throwable?)

    /** Журнала нет. */
    companion object {
        /** Ничего не записывает. */
        val Silent: CabinetJournal = CabinetJournal { _, _ -> }
    }
}
