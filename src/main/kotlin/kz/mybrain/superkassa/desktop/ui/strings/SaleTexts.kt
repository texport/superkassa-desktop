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
    /**
     * Блок покупателя: его ИИН или БИН и отраслевые реквизиты чека.
     *
     * Назван по тому, что в нём заполняют: «реквизиты чека» кассиру
     * не говорили, чьи это реквизиты, и скидка на чек попадала туда же
     * просто потому, что была реквизитом.
     */
    val customerData: String,

    /** Заголовок блока, где скидка и наценка на чек стоят вместе с их итогом. */
    val receiptChanges: String,

    /** Стоимость набранного до скидок и то, что вышло после них. */
    val changesBefore: String,
    val changesAfter: String,

    /** Сколько чек уже потерял скидками по строкам: вместе со скидкой на чек они запрещены. */
    val itemDiscountsGiven: String,

    /** Набранный процент — в тенге, и набранные тенге — долей: рядом с полем. */
    val changeAsSum: String,
    val changeAsPercent: String,
    val paymentAndTotal: String,
    val lineDiscount: String,
    val addByEnter: String,
    val notANumber: String,
    val pricePrecision: String,
    val quantityPrecision: String,

    /** Штучный товар дробным количеством не продаётся. */
    val quantityWhole: String,
    val needName: String,
    val needPrice: String,
    val needQuantity: String,
    val discountTooBig: String,

    /** Скидка со знаком минус — не скидка: она увеличивает строку. */
    val discountNegative: String,
    val barcodeHint: String,
    val paymentUnsupported: String,
    val takenOnlyCash: String,
    val binHint: String,
    val blockNoKkm: String,
    val blockNoPin: String,
    val blockKkmBlocked: String,
    val blockShiftClosed: String,
    val blockEmptyBasket: String,
    val blockZeroLine: String,
    val blockPaymentUnsupported: String,
    val blockDiscountScopes: String,

    /**
     * Скидка или наценка на чек со знаком минус.
     *
     * Отрицательная скидка молча прибавляла к итогу, а отрицательная
     * наценка — вычитала: кассир видел в «Итого» не ту сумму, которую
     * назвал, и объяснения этому на экране не было.
     */
    val blockDiscountNegative: String,

    /**
     * Процент скидки или наценки вне ста.
     *
     * Больше ста процентов узел не принимает ни у скидки, ни у наценки,
     * и узнать об этом кассир должен до нажатия.
     */
    val blockPercentRange: String,

    /** Скидка больше стоимости набранного: платить после неё было бы нечем. */
    val blockDiscountOverItems: String,
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
    val exciseDone: String,

    /**
     * Окно цены для позиции, найденной в каталоге без цены.
     *
     * Каталог описывает товар, а цену назначает продавец, поэтому окно
     * не сообщает об ошибке — оно спрашивает то, чего в каталоге нет.
     */
    val priceAsk: String,
    val priceAskHint: String,
    val priceAskCode: String,
    val priceAskCancel: String,

    /**
     * Подробности строки чека.
     *
     * Строка корзины показывает то, что влезает в неё, — а кассир, которого
     * спросили «а какой это НТИН?», обязан найти ответ, не открывая
     * справочник. Подписи полей общие с формой ввода, здесь только те,
     * которых у формы нет.
     */
    val positionDetails: String,
    val positionNameKk: String,
    val positionSum: String,
    val positionNtin: String,
    val positionSection: String,
    val positionStornoMarked: String,
    val positionClose: String
)

/** Надписи области на выбранном языке. */
fun saleTexts(language: Language): SaleTexts = when (language) {
    Language.Kk -> saleTextsKk
    Language.Ru -> saleTextsRu
    Language.En -> saleTextsEn
}
