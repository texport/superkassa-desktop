package kz.mybrain.superkassa.strings.api.journal

/** Экран возврата: выбор чека-основания и сумма возврата. */
data class ReturnJournalTexts(
    val itemsToReturn: String,
    val basis: String,
    val basisHint: String,
    val basisColumn: String,

    /**
     * Весь день показан: чеков-оснований больше нет.
     *
     * Своя строка, а не журнальная «Показан весь срок»: журнал листают
     * сроком, а основание ищут за один день, и о сроке под списком чеков
     * дня говорить нечего.
     */
    val allBasesShown: String,
    val chooseBasis: String,
    val chooseBasisHint: String,

    /** Назад к списку чеков: на узком окне панель возврата стоит вместо списка. */
    val backToList: String,
    val noBasisHint: String,

    /**
     * Чеки дня прочитать не удалось.
     *
     * Отдельно от «оснований нет»: касса отказала или не ответила, и о чеках
     * покупателя он не сказал ничего. Кассир при покупателе с чеком в руках
     * читал молчание кассы как отказ в возврате.
     */
    val basisUnread: String,
    val basisUnreadHint: String,
    val shiftClosedHint: String,
    val receiptTotal: String,
    val fiscalSign: String,
    val noSaleBasis: String,
    val noBuyBasis: String,
    val shiftClosed: String,

    /**
     * Касса заблокирована — в том числе снята с учёта.
     *
     * Касса в таком состоянии фискальных команд не проводит, а смена у неё
     * может оставаться открытой: без своего состояния экран предлагал
     * кассиру кнопку, на которую касса отвечает KKM_BLOCKED.
     */
    val kkmBlocked: String,
    val kkmBlockedHint: String,
    val amount: String,
    val wholeReceipt: String,
    val partialHint: String,
    val amountInvalid: String,
    val amountTooLarge: String,
    val amountEmpty: String,

    /**
     * Сумма возврата набрана не по отметкам.
     *
     * Отмеченные позиции уходят строками чека только тогда, когда сумма
     * возврата — это в точности их сумма. Иначе чек описывал бы строками
     * одну сумму, а оплатой другую, и ОФД принять его не может.
     */
    val itemsIgnored: String,

    /**
     * В ящике меньше денег, чем отдают покупателю наличными.
     *
     * Возврат продажи берёт деньги из того же ящика, из которого их
     * изымают, и о нехватке кассир должен узнать до того, как назовёт
     * сумму покупателю, а не из отказа кассы после.
     */
    val drawerShort: String
)
