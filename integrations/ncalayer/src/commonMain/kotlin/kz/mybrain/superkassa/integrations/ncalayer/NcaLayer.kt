package kz.mybrain.superkassa.integrations.ncalayer

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Подпись ЭЦП владельца через NCALayer.
 *
 * Ключ и пароль к нему остаются у владельца: приложение отправляет
 * в NCALayer то, что нужно подписать, а окно выбора сертификата и ввода
 * пароля показывает сам NCALayer. Ни ключ, ни пароль сюда не приходят.
 *
 * Один вызов — один документ: NCALayer показывает окно на каждую подпись,
 * и склеивать документы значило бы подписывать то, чего владелец не видел.
 */
interface NcaLayer {

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * Ждать приходится долго — владелец выбирает сертификат и вводит пароль, —
     * поэтому вызов длится до срока [NcaSettings.signWindow]. Отмена
     * вызывающего остаётся отменой: ни отказа, ни повтора за ней нет.
     *
     * @param payload то, что подписывается, в base64 — как его выдала служба.
     * @return CMS-подпись одной строкой base64, без переносов и обрамления PEM.
     * @throws NcaRefusal NCALayer не отвечает, молчит или подписи не дал.
     */
    suspend fun sign(payload: String): String
}

/**
 * Как обращаться к NCALayer.
 *
 * @property address где NCALayer слушает на этой машине.
 * @property origin чьё имя NCALayer показывает владельцу в окне подписи:
 *   латиницей и без пояснений, строка встаёт в его шаблон «{0} запрашивает
 *   разрешение».
 * @property locale язык окна подписи: `ru`, `kk` или `en` — язык владельца.
 * @property signWindow сколько ждать подписи. Столько же приложение
 *   отсчитывает владельцу: ожидание с видимым сроком он может переждать.
 * @property handshakeWait сколько ждать рукопожатия: столько занимает
 *   поднять защищённое соединение на петле, человек в этом не участвует.
 * @property windowShown с какой задержкой закрытое соединение означает
 *   закрытое владельцем окно: быстрее человек его не закроет — он его ещё
 *   не увидел, и тогда соединение закрыл сам NCALayer, не поняв запроса.
 */
data class NcaSettings(
    val address: String = "wss://127.0.0.1:13579",
    val origin: String = "Superkassa",
    val locale: String = "ru",
    val signWindow: Duration = 3.minutes,
    val handshakeWait: Duration = 5.seconds,
    val windowShown: Duration = 3.seconds
)

/**
 * Журнал обмена с NCALayer.
 *
 * Разбирают его с чужой машины, без отладочного режима, поэтому ход
 * обмена пишется всегда. В строки идут только адрес, модуль с методом,
 * состав полей кадра, его длина и имя помехи — ни подписи, ни содержимого
 * для подписи, ни сертификата.
 */
fun interface NcaJournal {

    /** Записывает строку журнала; [failure] — помеха, если шаг сорвался. */
    fun record(line: String, failure: Throwable?)

    /** Журнала нет. */
    companion object {
        /** Ничего не записывает. */
        val Silent: NcaJournal = NcaJournal { _, _ -> }
    }
}
