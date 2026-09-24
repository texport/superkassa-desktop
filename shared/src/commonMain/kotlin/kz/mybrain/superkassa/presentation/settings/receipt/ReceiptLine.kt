package kz.mybrain.superkassa.presentation.settings.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import kz.mybrain.superkassa.strings.api.common.SettingStrings

/**
 * Места печати своих строк кассы на чеке.
 *
 * Перечисление, а не девять одинаковых полей подряд: набор задан печатной
 * формой, и добавление места должно быть строкой здесь, а не копией
 * пятнадцати строк разметки. Порядок — тот, в каком строки встанут на чеке.
 */
enum class ReceiptLine(
    val title: (SettingStrings) -> String,
    val read: (ReceiptBrandingRequest) -> String?,
    val write: (ReceiptBrandingRequest, String) -> ReceiptBrandingRequest
) {
    BeforeHeader({ it.lineBeforeHeader }, { it.beforeHeaderMsg }, { b, v -> b.copy(beforeHeaderMsg = v) }),
    Header({ it.lineHeader }, { it.headerMsg }, { b, v -> b.copy(headerMsg = v) }),
    AfterHeader({ it.lineAfterHeader }, { it.afterHeaderMsg }, { b, v -> b.copy(afterHeaderMsg = v) }),
    BeforeItems({ it.lineBeforeItems }, { it.beforeItemsMsg }, { b, v -> b.copy(beforeItemsMsg = v) }),
    AfterItems({ it.lineAfterItems }, { it.afterItemsMsg }, { b, v -> b.copy(afterItemsMsg = v) }),
    BeforeTotals({ it.lineBeforeTotals }, { it.beforeTotalsMsg }, { b, v -> b.copy(beforeTotalsMsg = v) }),
    AfterTotals({ it.lineAfterTotals }, { it.afterTotalsMsg }, { b, v -> b.copy(afterTotalsMsg = v) }),
    BeforeQr({ it.lineBeforeQr }, { it.beforeQrMsg }, { b, v -> b.copy(beforeQrMsg = v) }),
    Footer({ it.lineFooter }, { it.footerMsg }, { b, v -> b.copy(footerMsg = v) })
}
