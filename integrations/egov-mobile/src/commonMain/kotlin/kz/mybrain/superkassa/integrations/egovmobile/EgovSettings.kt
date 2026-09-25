package kz.mybrain.superkassa.integrations.egovmobile

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Где посредник eGov mobile и сколько ждать.
 *
 * @property relay адрес посредника: он держит для eGov mobile данные
 *   на подпись и принимает от него подпись. Свой посредник (кабинет БФД)
 *   встаёт сюда же, если говорит тем же протоколом.
 * @property signWindow сколько ждать подписи: владелец открывает eGov mobile,
 *   входит в него и подписывает.
 * @property answerWait сколько ждать ответа на регистрацию и передачу данных.
 * @property retryPause пауза перед новым долгим запросом подписи, когда
 *   прежний оборвался.
 */
data class EgovSettings(
    val relay: String = SIGEX,
    val signWindow: Duration = 3.minutes,
    val answerWait: Duration = 30.seconds,
    val retryPause: Duration = 1.seconds
) {
    /** Посредник по умолчанию. */
    companion object {
        /** Публичный базовый API SIGEX для eGov mobile (`/api/egovQr`). */
        const val SIGEX: String = "https://sigex.kz"
    }
}

/**
 * Как подпись называется в eGov mobile: приложение показывает владельцу,
 * кто и что просит подписать.
 *
 * @property description кто просит — строка процедуры в eGov mobile.
 * @property nameRu название документа по-русски; [nameKk] и [nameEn] — по-казахски
 *   и по-английски: eGov mobile показывает его на языке владельца.
 */
data class EgovDocument(val description: String, val nameRu: String, val nameKk: String, val nameEn: String)

/**
 * Журнал обмена с посредником.
 *
 * В строку идут шаг, код ответа и длительность — ни данных на подпись,
 * ни подписи, ни адресов процедуры: по адресу процедуры подпись можно забрать.
 */
fun interface EgovJournal {

    /** Записывает строку обмена; [failure] — помеха, если ответа не было. */
    fun record(line: String, failure: Throwable?)

    /** Журнала нет. */
    companion object {
        /** Ничего не записывает. */
        val Silent: EgovJournal = EgovJournal { _, _ -> }
    }
}
