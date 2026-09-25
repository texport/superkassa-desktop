package kz.mybrain.superkassa.presentation.setup.registration

import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kotlin.time.Duration

/**
 * Касса в кабинете и заявление о постановке её на учёт.
 *
 * @property registerId о какой кассе в кабинете прочитано: мастер, начатый
 *   заново, заводит другую, и прочитанное о прежней ей не в счёт.
 * @property record касса в кабинете со слов кабинета; `null` — ещё не прочитана.
 * @property signing заявление ждёт подписи владельца: на месте кнопок идёт срок.
 */
data class RegistrationUiState(
    val registerId: String? = null,
    val record: CabinetRecord? = null,
    val signing: Boolean = false
) {
    /** Прочитанное о кассе [registerId]; о другой кассе — `null`. */
    fun recordOf(registerId: String?): CabinetRecord? =
        record.takeIf { registerId != null && registerId == this.registerId }

    /** Касса [registerId] встала на учёт: можно выпускать токен и заводить её здесь. */
    fun onRecord(registerId: String?): Boolean = recordOf(registerId)?.onRecord == true
}

/**
 * Что владелец может сделать на шаге постановки на учёт.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface RegistrationActions {

    /** Перечитывает кассу в кабинете: встала ли она на учёт. */
    fun readRecord(registerId: String) = Unit

    /**
     * Перечитывает кассу в кабинете каждые [every], пока номера КГД нет.
     *
     * Ответ КГД приходит через десятки секунд, и владелец сидел над шагом,
     * нажимая «Обновить». Опрос живёт, пока его ждёт экран: закрытый мастер
     * кабинет не спрашивает.
     */
    suspend fun watchRecord(registerId: String, every: Duration) = Unit

    /** Готовит, подписывает и подаёт заявление о постановке на учёт. */
    fun submit(registerId: String) = Unit

    /** Прерывает ожидание подписи: заявление остаётся черновиком в кабинете. */
    fun cancelSubmit() = Unit
}
