package kz.mybrain.superkassa.domain.setup.port

import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.model.RegistrationToSign

/**
 * Кабинет мастера для проверок: отвечает заданным и записывает, о чём спросили.
 *
 * @property record касса в кабинете; по умолчанию — черновик без номера КГД.
 * @property token выпускаемый токен; `null` — кабинет его не выдаёт.
 * @property signer подпись владельца: проверка подставляет молчание или отказ.
 */
class FakeSetupCabinet(
    var record: CabinetRecord = CabinetRecord(status = "DRAFT", registrationNumber = null),
    var token: String? = null,
    var signer: suspend (String) -> String = { "signed-$it" }
) : SetupCabinet {
    /** Обращения по порядку: `record r-1`, `prepare r-1`, `send r-1 a-1 signed-…`, `token r-1`. */
    val asked = mutableListOf<String>()

    override suspend fun record(registerId: String): CabinetRecord = record.also { asked += "record $registerId" }

    override suspend fun prepareRegistration(registerId: String): RegistrationToSign {
        asked += "prepare $registerId"
        return RegistrationToSign(actionId = ACTION, payload = PAYLOAD)
    }

    override suspend fun sign(payload: String): String = signer(payload)

    override suspend fun sendRegistration(registerId: String, actionId: String, signature: String) {
        asked += "send $registerId $actionId $signature"
    }

    override suspend fun issueToken(registerId: String): String? = token.also { asked += "token $registerId" }

    companion object {
        const val ACTION = "a-1"
        const val PAYLOAD = "cGF5bG9hZA=="
    }
}
