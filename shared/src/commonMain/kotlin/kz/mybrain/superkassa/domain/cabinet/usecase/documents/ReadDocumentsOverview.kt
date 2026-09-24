package kz.mybrain.superkassa.domain.cabinet.usecase.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments

/** Счётчики документов кассы за всё время. */
class ReadDocumentsOverview(private val documents: CabinetDocuments) {
    suspend operator fun invoke(registerId: String): DocumentsOverview = documents.overview(registerId)
}
