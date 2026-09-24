package kz.mybrain.superkassa.domain.print.model

import io.github.texport.superkassa.core.presentation.api.PrintApi

/**
 * Что рисуется печатной формой.
 *
 * Своих документов касса рисует по номеру в журнале, документы кабинета —
 * по пакету протокола: пробиты они могли быть на другой машине, и номера
 * в журнале этой кассы у них нет.
 *
 * @property name как назвать файл формы, если имени у документа нет.
 */
sealed interface PrintSource {
    val name: String

    /** Документ журнала этой кассы: чек, отчёт, внесение, смена. */
    data class Journal(val documentId: String) : PrintSource {
        override val name: String get() = documentId
    }

    /** Документ, переданный пакетом протокола — запросом кассы и ответом ОФД. */
    data class Packet(val packet: String, override val name: String) : PrintSource {
        override fun toString(): String = "Packet(name=$name)"
    }
}

/**
 * Печатная форма в нужном виде — у кассы [kkmId] по пину [pin].
 *
 * Рисует касса, а не приложение: свой рисунок дал бы два разных чека
 * по одному документу.
 */
internal fun PrintSource.render(api: PrintApi, kkmId: String, pin: String, kind: PrintKind): ByteArray = when (this) {
    is PrintSource.Journal -> when (kind) {
        PrintKind.Png -> api.getDocumentPrintPng(kkmId, documentId, pin)
        PrintKind.Pdf -> api.getDocumentPrintPdf(kkmId, documentId, pin)
        PrintKind.Html -> api.getDocumentPrintHtml(kkmId, documentId, pin).encodeToByteArray()
    }
    is PrintSource.Packet -> when (kind) {
        PrintKind.Png -> api.getProtocolPrintPng(kkmId, pin, packet)
        PrintKind.Pdf -> api.getProtocolPrintPdf(kkmId, pin, packet)
        PrintKind.Html -> api.getProtocolPrintHtml(kkmId, pin, packet).encodeToByteArray()
    }
}
