package kz.mybrain.superkassa.data.kassa

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Касса, поднятая в процессе приложения.
 *
 * Единственное место, знающее, что фасад ядра блокирующий: вызов уходит
 * в [io], и экраны зовут кассу из любой корутины.
 *
 * @param io где выполнять вызовы; проверкам подставляется свой диспетчер.
 */
class EmbeddedKassa(
    private val api: SuperkassaApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : Kassa {

    override suspend fun <T> call(request: (SuperkassaApi) -> T): T = withContext(io) { request(api) }

    companion object {
        /**
         * С каким ОФД и по какой версии протокола работает касса.
         *
         * Умолчаний у ядра нет намеренно: касса в приложении не должна
         * молча работать по другой версии, чем узел той же установки.
         * Провайдер — БФД: адреса его контуров знает ядро.
         */
        fun config(): SuperkassaConfig =
            SuperkassaConfig(ofdProviderId = OFD_PROVIDER, ofdProtocolVersion = PROTOCOL_VERSION)

        private const val OFD_PROVIDER = "BFD"
        private const val PROTOCOL_VERSION = "204"
    }
}
