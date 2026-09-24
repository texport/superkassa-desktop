package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Строки чека-основания: вернуть можно только проданное и теми же строками. */
class ReadBasisItems(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(documentId: String): Answer<List<ReceiptItemView>> =
        kassa.askSeated(signed) { api, seat -> api.getDocumentDetails(seat.kkmId, documentId, seat.pin) }
            .map { it.items }
}
