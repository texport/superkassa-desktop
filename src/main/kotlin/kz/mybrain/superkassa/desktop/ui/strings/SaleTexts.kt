package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Продажа и корзина».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Причина недоступности кнопки и объяснение отказа узла — намеренно одни
 * и те же слова: кассир, прочитавший «смена закрыта» до нажатия, узнаёт ту
 * же фразу, если отказ всё-таки пришёл от узла.
 */
data class SaleTexts(
    val basketEmpty: String,
    val basketEmptyHint: String,
    val positionsCount: String,
    val itemsSum: String,
    val change: String,
    val changeNone: String,
    val positionEntry: String,
    val receiptDetails: String,
    val paymentAndTotal: String,
    val lineDiscount: String,
    val addByEnter: String,
    val notANumber: String,
    val pricePrecision: String,
    val quantityPrecision: String,
    val needName: String,
    val needPrice: String,
    val needQuantity: String,
    val discountTooBig: String,
    val barcodeHint: String,
    val paymentUnsupported: String,
    val takenOnlyCash: String,
    val binHint: String,
    val fillIn: String,
    val numberField: String,
    val parkingHint: String,
    val blockNoKkm: String,
    val blockNoPin: String,
    val blockKkmBlocked: String,
    val blockShiftClosed: String,
    val blockEmptyBasket: String,
    val blockPaymentUnsupported: String,
    val blockDiscountScopes: String,
    val blockTotalNotPositive: String,
    val blockTakenTooSmall: String,
    val blockBin: String,
    val refusalVatGroup: String,
    val refusalNoCash: String,
    val excise: String,
    val exciseHint: String,
    val exciseEmpty: String,
    val exciseCount: String,
    val exciseRepeated: String,
    val exciseTooLong: String,
    val exciseDone: String
)

/** Надписи области на выбранном языке. */
fun saleTexts(language: Language): SaleTexts = when (language) {
    Language.Kk -> saleTextsKk
    Language.Ru -> saleTextsRu
    Language.En -> saleTextsEn
}
