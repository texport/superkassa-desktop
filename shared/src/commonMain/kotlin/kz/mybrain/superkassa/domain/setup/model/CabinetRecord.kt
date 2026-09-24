package kz.mybrain.superkassa.domain.setup.model

/**
 * Касса в кабинете, какой её видит мастер.
 *
 * @property status код состояния учёта со слов кабинета: `DRAFT`, `REGISTERED`.
 * @property registrationNumber номер КГД; `null` — касса ещё не на учёте.
 */
data class CabinetRecord(val status: String, val registrationNumber: String?) {

    /** Касса встала на учёт: КГД выдал ей номер. */
    val onRecord: Boolean get() = !registrationNumber.isNullOrBlank()
}

/**
 * Заявление, подготовленное кабинетом.
 *
 * @property actionId под каким действием кабинет ждёт подписанное.
 * @property payload что подписывает владелец, в base64.
 */
data class RegistrationToSign(val actionId: String, val payload: String) {
    override fun toString(): String = "RegistrationToSign(actionId=$actionId)"
}
