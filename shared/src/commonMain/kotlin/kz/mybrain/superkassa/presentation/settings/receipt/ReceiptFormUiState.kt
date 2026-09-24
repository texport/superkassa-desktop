package kz.mybrain.superkassa.presentation.settings.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import io.github.texport.superkassa.core.presentation.api.model.reference.PaperWidthResponse
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.domain.settings.model.brandingRequest

/**
 * Печатная форма чека кассы: язык, ширина ленты, реклама ОФД и свои строки.
 *
 * @property paperWidths макеты из справочника кассы: свой список не узнал
 *   бы о новой ширине ленты, пока приложение не перевыпустят.
 * @property lineDrafts набранные, но не сохранённые свои строки. Нетронутая
 *   строка остаётся как есть: касса отдаёт ненабранную пустотой, и пустая
 *   строка поверх неё зажигала бы «Сохранить» там, где ничего не меняли.
 */
data class ReceiptFormUiState(
    val kkm: KkmResponse? = null,
    val paperWidths: List<PaperWidthResponse> = emptyList(),
    val lineDrafts: Map<ReceiptLine, String> = emptyMap(),
    val busy: Boolean = false
) {
    /** Оформление, как оно у кассы сейчас. */
    val branding: ReceiptBrandingRequest get() = kkm?.brandingRequest ?: ReceiptBrandingRequest()

    /** Оформление с набранными строками. */
    val edited: ReceiptBrandingRequest
        get() = lineDrafts.entries.fold(branding) { carried, (line, text) -> line.write(carried, text) }

    /** Касса меняет оформление только в режиме программирования. */
    val editable: Boolean get() = kkm?.isProgramming == true

    /** Макеты на выбор: из справочника кассы, а без него — свои. */
    val layoutCodes: List<String> get() = paperWidths.map { it.code }.ifEmpty { PrintLayout.entries.map { it.code } }
}
