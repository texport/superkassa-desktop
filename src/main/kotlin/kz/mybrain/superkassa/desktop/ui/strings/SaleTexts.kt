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
    val refusalNoCash: String
)

internal val saleTextsRu = SaleTexts(
    basketEmpty = "Корзина пуста",
    basketEmptyHint = "Отсканируйте штрихкод или введите позицию вручную — Enter добавит её в чек.",
    positionsCount = "Позиций",
    itemsSum = "Сумма позиций",
    change = "Сдача",
    changeNone = "Сдачи нет: принято ровно в итог",
    positionEntry = "Новая позиция",
    receiptDetails = "Реквизиты чека",
    paymentAndTotal = "Оплата и итог",
    lineDiscount = "скидка",
    addByEnter = "Enter добавляет позицию",
    notANumber = "Это не число",
    pricePrecision = "Цена задаётся до тиына — не больше двух цифр после запятой",
    quantityPrecision = "Количество задаётся до тысячной доли",
    needName = "Введите наименование товара",
    needPrice = "Цена должна быть больше нуля",
    needQuantity = "Количество должно быть больше нуля",
    discountTooBig = "Скидка не может быть больше стоимости позиции",
    barcodeHint = "Сканер вводит код и нажимает Enter сам",
    paymentUnsupported = "протокол 2.0.4 его не принимает",
    takenOnlyCash = "«Принято» и сдача считаются только для наличных",
    binHint = "Ровно 12 цифр",
    fillIn = "Заполните",
    numberField = "%s — это число",
    parkingHint = "Время въезда отсчитывается назад от текущего момента",
    blockNoKkm = "Касса не выбрана",
    blockNoPin = "Кассир не вошёл: введите пин",
    blockKkmBlocked = "Касса заблокирована — обратитесь в обслуживание",
    blockShiftClosed = "Смена закрыта — откройте смену на главной",
    blockEmptyBasket = "В чеке нет ни одной позиции",
    blockPaymentUnsupported = "Этот вид оплаты узел не примет: протокол 2.0.4 его не поддерживает",
    blockDiscountScopes = "Скидка на позицию и скидка на весь чек вместе запрещены — оставьте одну",
    blockTotalNotPositive = "Итог чека должен быть больше нуля",
    blockTakenTooSmall = "Принято меньше итога: пересчитайте деньги покупателя",
    blockBin = "ИИН/БИН покупателя — ровно 12 цифр",
    refusalVatGroup = "Такую ставку НДС узел не знает",
    refusalNoCash = "В денежном ящике недостаточно наличных"
)

internal val saleTextsKk = SaleTexts(
    basketEmpty = "Себет бос",
    basketEmptyHint = "Штрих-кодты сканерлеңіз немесе позицияны қолмен енгізіңіз — Enter оны чекке қосады.",
    positionsCount = "Позиция саны",
    itemsSum = "Позициялар сомасы",
    change = "Қайтарым",
    changeNone = "Қайтарым жоқ: сома дәл қабылданды",
    positionEntry = "Жаңа позиция",
    receiptDetails = "Чек деректемелері",
    paymentAndTotal = "Төлем және қорытынды",
    lineDiscount = "жеңілдік",
    addByEnter = "Enter позицияны қосады",
    notANumber = "Бұл сан емес",
    pricePrecision = "Баға тиынға дейін беріледі — үтірден кейін екі таңбадан аспайды",
    quantityPrecision = "Саны мыңнан бір үлеске дейін беріледі",
    needName = "Тауар атауын енгізіңіз",
    needPrice = "Баға нөлден үлкен болуы тиіс",
    needQuantity = "Саны нөлден үлкен болуы тиіс",
    discountTooBig = "Жеңілдік позиция құнынан асып кете алмайды",
    barcodeHint = "Сканер кодты өзі енгізіп, Enter басады",
    paymentUnsupported = "2.0.4 хаттамасы оны қабылдамайды",
    takenOnlyCash = "«Қабылданды» мен қайтарым тек қолма-қол ақшаға есептеледі",
    binHint = "Дәл 12 цифр",
    fillIn = "Толтырыңыз",
    numberField = "%s — бұл сан",
    parkingHint = "Кіру уақыты ағымдағы сәттен кері есептеледі",
    blockNoKkm = "Касса таңдалмаған",
    blockNoPin = "Кассир кірмеген: пин енгізіңіз",
    blockKkmBlocked = "Касса бұғатталған — қызмет көрсетуге хабарласыңыз",
    blockShiftClosed = "Ауысым жабық — басты бетте ауысымды ашыңыз",
    blockEmptyBasket = "Чекте бірде-бір позиция жоқ",
    blockPaymentUnsupported = "Бұл төлем түрін түйін қабылдамайды: 2.0.4 хаттамасы оны қолдамайды",
    blockDiscountScopes = "Позицияға және бүкіл чекке жеңілдік бірге қолданылмайды — біреуін қалдырыңыз",
    blockTotalNotPositive = "Чек қорытындысы нөлден үлкен болуы тиіс",
    blockTakenTooSmall = "Қабылданған сома қорытындыдан аз: сатып алушының ақшасын қайта санаңыз",
    blockBin = "Сатып алушының ЖСН/БСН — дәл 12 цифр",
    refusalVatGroup = "Мұндай ҚҚС мөлшерлемесін түйін білмейді",
    refusalNoCash = "Ақша жәшігінде қолма-қол ақша жеткіліксіз"
)

internal val saleTextsEn = SaleTexts(
    basketEmpty = "The basket is empty",
    basketEmptyHint = "Scan a barcode or type an item — Enter adds it to the receipt.",
    positionsCount = "Items",
    itemsSum = "Items subtotal",
    change = "Change",
    changeNone = "No change: tendered exactly the total",
    positionEntry = "New item",
    receiptDetails = "Receipt details",
    paymentAndTotal = "Payment and total",
    lineDiscount = "discount",
    addByEnter = "Enter adds the item",
    notANumber = "Not a number",
    pricePrecision = "Price goes down to the tiyn — at most two decimals",
    quantityPrecision = "Quantity goes down to a thousandth",
    needName = "Enter the item name",
    needPrice = "Price must be above zero",
    needQuantity = "Quantity must be above zero",
    discountTooBig = "The discount cannot exceed the line total",
    barcodeHint = "The scanner types the code and presses Enter itself",
    paymentUnsupported = "protocol 2.0.4 does not accept it",
    takenOnlyCash = "Tendered and change apply to cash only",
    binHint = "Exactly 12 digits",
    fillIn = "Fill in",
    numberField = "%s must be a number",
    parkingHint = "Entry time counts back from now",
    blockNoKkm = "No register selected",
    blockNoPin = "The cashier is not signed in: enter the PIN",
    blockKkmBlocked = "The register is blocked — call service",
    blockShiftClosed = "The shift is closed — open it on the dashboard",
    blockEmptyBasket = "The receipt has no items",
    blockPaymentUnsupported = "The node will refuse this payment type: protocol 2.0.4 does not support it",
    blockDiscountScopes = "An item discount and a receipt discount cannot be combined — keep one",
    blockTotalNotPositive = "The receipt total must be above zero",
    blockTakenTooSmall = "Tendered less than the total: recount the customer's money",
    blockBin = "The customer IIN/BIN is exactly 12 digits",
    refusalVatGroup = "The node does not know this VAT rate",
    refusalNoCash = "Not enough cash in the drawer"
)

/** Надписи области на выбранном языке. */
fun saleTexts(language: Language): SaleTexts = when (language) {
    Language.Kk -> saleTextsKk
    Language.Ru -> saleTextsRu
    Language.En -> saleTextsEn
}
