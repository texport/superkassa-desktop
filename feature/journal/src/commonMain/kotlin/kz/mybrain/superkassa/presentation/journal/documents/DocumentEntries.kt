package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.document.model.hasOwnAmount
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.document.model.printable
import kz.mybrain.superkassa.domain.document.model.refusalCode
import kz.mybrain.superkassa.domain.journal.model.ReceiptDeliveryRules
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.presentation.common.document.JournalEntry
import kz.mybrain.superkassa.presentation.common.document.JournalType
import kz.mybrain.superkassa.presentation.common.document.deliveryOf
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Документы кассы строками общего журнала.
 *
 * Касса — первый из двух источников журнала; второй, кабинет, приводит
 * свои записи к тем же строкам. Дальше показ, поиск и отбор одинаковы,
 * и то, что кассир видит у кассы, владелец находит в кабинете.
 *
 * @param names названия видов документа со слов кассы.
 */
fun journalEntriesOf(
    texts: AppStrings,
    language: Language,
    names: Map<String, TrilingualMessageResponse>,
    documents: List<FiscalDocumentResponse>
): List<JournalEntry> = documents.map { document ->
    JournalEntry(
        key = document.id,
        at = document.createdAt,
        moment = Dates.stamp(document.createdAt),
        typeCode = document.docType,
        type = documentTypeTitle(document.docType, names, language, texts.enums),
        number = document.number?.toString() ?: Glyphs.DASH,
        numberOrder = document.number,
        // У отчёта и открытия смены своей суммы нет: в журнале стояло
        // «0,00 ₸» — читается как «не продано ничего». Итоги смены лежат
        // в самом отчёте, а здесь на их месте прочерк.
        amount = if (document.hasOwnAmount) Money.formatTiyn(document.totalAmount) else Glyphs.DASH,
        amountOrder = document.totalAmount?.takeIf { document.hasOwnAmount }?.let(Tenge::decimal),
        // Автономный признак показан наравне с фискальным: у документа,
        // пробитого без связи, фискального признака ещё нет, и пустая
        // клетка выглядела бы утратой документа.
        sign = document.fiscalSign ?: document.autonomousSign ?: Glyphs.DASH,
        delivery = deliveryOf(document),
        shiftNo = document.shiftNo,
        refusal = refusalOf(document, texts, language),
        printable = document.printable,
        openable = ReceiptDeliveryRules.delivers(document)
    )
}

/** Виды документов, встретившиеся среди [documents], для отбора — в порядке справочника кассы. */
internal fun journalTypesOf(
    texts: AppStrings,
    language: Language,
    names: Map<String, TrilingualMessageResponse>,
    documents: List<FiscalDocumentResponse>
): List<JournalType> = documentTypesIn(documents.map { it.docType }, names.keys.toList())
    .map { code -> JournalType(code, documentTypeTitle(code, names, language, texts.enums)) }

/**
 * Почему документ отвергнут — одной строкой для подсказки и поиска.
 *
 * Слова по коду отказа — те же, что на главном экране: кассир стоит перед
 * покупателем, и «Same customer and taxpayer IIN» ему не помогает. Для
 * незнакомого кода остаётся пояснение БФД: молчать об отказе хуже, чем
 * сказать о нём чужими словами. Код идёт следом — с ним идут
 * в обслуживание.
 */
private fun refusalOf(document: FiscalDocumentResponse, texts: AppStrings, language: Language): String? {
    val words = textsOf(language).journal.ofdRefusal.words(document.refusalCode)
        ?: document.ofdErrorText?.takeIf { it.isNotBlank() }
    val code = document.refusalCode?.let { "${texts.common.refusalCode} $it" }
    val reason = listOfNotNull(words, code)
    return reason.takeIf { it.isNotEmpty() }?.joinToString(Glyphs.SEPARATOR)
}
