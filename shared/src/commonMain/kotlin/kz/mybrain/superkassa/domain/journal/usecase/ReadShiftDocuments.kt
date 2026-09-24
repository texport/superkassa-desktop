package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.journal.model.DocumentPages
import kz.mybrain.superkassa.domain.journal.model.Paged
import kz.mybrain.superkassa.domain.journal.model.withPage
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Следующая страница документов одной смены.
 *
 * Смена оживлённой кассы длиннее одной страницы, и её конец иначе молча
 * пропадал бы.
 */
class ReadShiftDocuments(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return прочитанное с новой страницей. */
    suspend operator fun invoke(
        shiftId: String,
        shown: List<FiscalDocumentResponse>
    ): Answer<Paged<FiscalDocumentResponse>> {
        val size = DocumentPages.DOCUMENTS
        return kassa.askSeated(signed) { api, seat ->
            api.listShiftDocuments(seat.kkmId, shiftId, size, shown.size, seat.pin)
        }.map { shown.withPage(it, size) { document -> document.id } }
    }
}
