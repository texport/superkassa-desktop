package kz.mybrain.superkassa.domain.setup.usecase

import kz.mybrain.superkassa.domain.setup.port.SetupCabinet

/**
 * Готовит заявление о постановке на учёт, отдаёт его на подпись и подаёт.
 *
 * Между подготовкой и подачей стоит владелец с ключом: закроет он окно
 * подписи или прервёт ожидание — заявление останется черновиком
 * в кабинете, в КГД не уйдёт ничего, и подать его можно заново.
 */
class SubmitRegistration(private val cabinet: SetupCabinet) {

    /** @param signing владелец подписывает (`true`) или уже нет: экран показывает срок подписи. */
    suspend operator fun invoke(registerId: String, signing: (Boolean) -> Unit) {
        val prepared = cabinet.prepareRegistration(registerId)
        signing(true)
        val signature = try {
            cabinet.sign(prepared.payload)
        } finally {
            signing(false)
        }
        cabinet.sendRegistration(registerId, prepared.actionId, signature)
    }
}
