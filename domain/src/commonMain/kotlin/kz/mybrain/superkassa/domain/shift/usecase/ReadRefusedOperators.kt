package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Кто оформил отклонённые документы: без имени отказ — «кто-то в 14:53».
 *
 * Молча: это подробность. Уже известные имена не перечитываются.
 */
class ReadRefusedOperators(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(
        refused: List<FiscalDocumentResponse>,
        known: Map<String, String>
    ): Map<String, String> = refused.filter { it.id !in known }.mapNotNull { document ->
        val details = kassa.askSeated(signed) { api, seat -> api.getDocumentDetails(seat.kkmId, document.id, seat.pin) }
        (details as? Answer.Done)?.value?.operatorName?.let { document.id to it }
    }.toMap()
}
