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
 * Следующая страница документов за срок у кассы, за которой работают.
 *
 * Страница не больше того, что касса отдаёт за раз; смещение — сколько
 * уже прочитано.
 */
class ReadPeriodDocuments(private val kassa: Kassa, private val signed: SignedKkm) {

    /**
     * @param range начало первых суток срока и начало суток за его концом, в миллисекундах.
     * @param shown прочитанное до сих пор.
     * @return прочитанное с новой страницей.
     */
    suspend operator fun invoke(
        range: LongRange,
        shown: List<FiscalDocumentResponse>
    ): Answer<Paged<FiscalDocumentResponse>> {
        val size = DocumentPages.DOCUMENTS
        return kassa.askSeated(signed) { api, seat ->
            api.listFiscalDocumentsByPeriod(seat.kkmId, range.first, range.last, size, shown.size, seat.pin)
        }.map { shown.withPage(it, size) { document -> document.id } }
    }
}
