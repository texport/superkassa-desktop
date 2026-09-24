package kz.mybrain.superkassa.presentation.strings.kassa

import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.presentation.strings.common.ReturnStrings
import kz.mybrain.superkassa.presentation.strings.common.SaleStrings
import kz.mybrain.superkassa.presentation.strings.journal.ReturnJournalTexts

/** Направление возврата словами кассира: возврат продажи выдаёт деньги, возврат покупки — принимает. */
fun ReturnKind.title(texts: ReturnStrings): String = when (this) {
    ReturnKind.Sell -> texts.saleReturn
    ReturnKind.Buy -> texts.purchaseReturn
}

/** Одно слово для сегмента: «Возврат» стоит заголовком рядом. */
fun ReturnKind.shortTitle(texts: SaleStrings): String = when (this) {
    ReturnKind.Sell -> texts.sale
    ReturnKind.Buy -> texts.purchase
}

/** Надпись кнопки возврата: отдать деньги или принять их. */
fun ReturnKind.action(texts: ReturnStrings): String = when (this) {
    ReturnKind.Sell -> texts.giveBack
    ReturnKind.Buy -> texts.takeBack
}

/** Что сказать, когда чеков-оснований такого возврата за день нет. */
fun ReturnKind.emptyText(texts: ReturnJournalTexts): String = when (this) {
    ReturnKind.Sell -> texts.noSaleBasis
    ReturnKind.Buy -> texts.noBuyBasis
}
