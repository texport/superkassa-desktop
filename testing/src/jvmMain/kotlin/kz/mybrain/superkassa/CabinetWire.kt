package kz.mybrain.superkassa

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings

/**
 * Обмен проверки с кабинетом: адрес и обмен, поверх которых порты
 * собираются на модуле кабинета так же, как в приложении (см. [SignedCabinet]).
 *
 * @property baseUrl адрес кабинета; недоступный — для проверок отказа связи.
 * @property http обмен проверки: подставной с заданными ответами, а без него —
 *   настоящий, чтобы недоступный адрес и был недоступен.
 */
class CabinetWire(
    val baseUrl: String = CabinetSettings.DEFAULT_URL,
    val http: HttpClient = HttpClient(CIO) { expectSuccess = false }
) {
    /** Разбор тел так, как его ведёт кабинет: лишние поля не мешают. */
    companion object {
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}
