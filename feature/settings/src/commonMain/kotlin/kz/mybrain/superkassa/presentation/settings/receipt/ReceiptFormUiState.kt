package kz.mybrain.superkassa.presentation.settings.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import io.github.texport.superkassa.core.presentation.api.model.reference.PaperWidthResponse
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.domain.settings.model.brandingRequest
import kz.mybrain.superkassa.presentation.settings.KkmDrafts

/**
 * Печатная форма чека кассы: язык, ширина ленты, реклама ОФД и свои строки.
 *
 * @property paperWidths макеты из справочника кассы: свой список не узнал
 *   бы о новой ширине ленты, пока приложение не перевыпустят.
 * @property drafts набранные, но не сохранённые свои строки — у каждой
 *   кассы свои: уход к другой кассе их не стирает.
 */
data class ReceiptFormUiState(
    val kkm: KkmResponse? = null,
    val paperWidths: List<PaperWidthResponse> = emptyList(),
    val drafts: KkmDrafts<Map<ReceiptLine, String>> = KkmDrafts(),
    val busy: Boolean = false
) {
    /**
     * Набранные строки этой кассы. Нетронутая строка остаётся как есть:
     * касса отдаёт ненабранную пустотой, и пустая строка поверх неё
     * зажигала бы «Сохранить» там, где ничего не меняли.
     */
    val lineDrafts: Map<ReceiptLine, String> get() = drafts.of(kkm?.kkmId).orEmpty()

    /** Строка этой кассы набрана. */
    fun typed(line: ReceiptLine, text: String): ReceiptFormUiState =
        copy(drafts = drafts.with(kkm?.kkmId, lineDrafts + (line to text)))

    /** Строки этой кассы сохранены: черновик забыт. */
    fun saved(): ReceiptFormUiState = copy(drafts = drafts.with(kkm?.kkmId, null))

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
