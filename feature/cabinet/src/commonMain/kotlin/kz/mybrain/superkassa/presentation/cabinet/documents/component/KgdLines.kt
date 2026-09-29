package kz.mybrain.superkassa.presentation.cabinet.documents.component

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.KgdDelivery
import kz.mybrain.superkassa.presentation.cabinet.documents.journal
import kz.mybrain.superkassa.presentation.cabinet.documents.words
import kz.mybrain.superkassa.presentation.common.document.JournalDeliveryChip
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/** Плашка состояния документа в КГД — те же слова и цвет, что в строке журнала. */
@Composable
internal fun KgdChip(kgd: KgdDelivery, texts: CabinetTexts) {
    JournalDeliveryChip(kgd.journal(), words = kgd.words(texts))
}

/**
 * Доставка документа в КГД строками карточки: состояние, когда КГД
 * ответил, и — если отклонил — почему.
 *
 * Прежде в карточке стояла только плашка, и отказ КГД оставался без
 * объяснения: чинить было нечего, потому что неизвестно что.
 *
 * @param resultAt когда КГД ответил; пусто — ответа ещё нет.
 * @param message почему КГД отклонил документ; пусто — не отклонял.
 */
@Composable
internal fun KgdLines(kgd: KgdDelivery, resultAt: String?, message: String?, texts: CabinetTexts) {
    val at = Dates.momentOf(resultAt).takeIf { it.isNotBlank() && !resultAt.isNullOrBlank() }
    DetailLine(texts.documents.kgdDelivery, listOfNotNull(kgd.words(texts), at).joinToString(Glyphs.SEPARATOR))
    DetailLine(texts.documents.kgdReason, message)
}
