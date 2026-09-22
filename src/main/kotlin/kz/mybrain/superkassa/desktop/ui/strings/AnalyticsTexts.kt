package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи раздела «Аналитика» личного кабинета.
 *
 * Раздел отвечает на три вопроса владельца сети: где стоят кассы,
 * откуда они выходят на связь и чем торгуют. Разговор здесь свой —
 * источники положения, кассы без места на карте, адреса обмена,
 * выручка и виды расчётов, — и в наборе кабинета эти полсотни строк
 * терялись бы среди заявлений и чеков.
 *
 * Значения вынесены по языкам в соседние файлы, как у остальных наборов.
 */
data class AnalyticsTexts(
    val title: String,
    val mapTab: String,
    val exchangeTab: String,

    val positionSource: String,
    val sourceAddress: String,
    val sourceCabinet: String,
    val sourceKkm: String,
    val sourceAddressHint: String,
    val sourceCabinetHint: String,
    val sourceKkmHint: String,

    val refresh: String,
    val unreachable: String,
    val unreachableHint: String,
    val notDeployed: String,
    val notDeployedHint: String,
    val refused: String,

    val placed: String,
    val withoutPosition: String,
    val kkmCount: String,
    val kkmColumn: String,
    val addressCount: String,

    val mapEmpty: String,
    val mapEmptyHint: String,

    /** Адреса касс ещё ищутся на карте: ждать, а не чинить. */
    val mapSearching: String,
    val mapSearchingHint: String,

    val pickPin: String,
    val pickPinHint: String,

    /** Карта во всё окно и возврат из него: подписи кнопки на самой карте. */
    val mapFullscreen: String,
    val mapFullscreenExit: String,

    val searchKkm: String,
    val searchKkmLabel: String,
    val allPlaces: String,
    val markShiftOpen: String,
    val markBlocked: String,
    val markOffRecord: String,
    val sieveEmpty: String,
    val sieveEmptyHint: String,
    val sieveClear: String,
    val kkmsHere: String,
    val openKkmSales: String,
    val kkmSalesTitle: String,

    /**
     * В списке рядом с картой нет ни одной кассы.
     *
     * Список держит все кассы компании, а не только непоставленные,
     * и пустым он бывает лишь у владельца без единой кассы. Прежняя
     * надпись «Все кассы на карте» стояла рядом со счётчиком «Касс · 0».
     */
    val kkmListEmpty: String,
    val kkmListEmptyHint: String,
    val reasonNoAddress: String,
    val reasonNoCabinetPoint: String,
    val reasonNoKkmPoint: String,
    val reasonSearching: String,
    val reasonNotOnMap: String,

    val registrationNumber: String,
    val noRegistrationNumber: String,
    val retailPlace: String,
    val blocked: String,
    val lastContact: String,
    val neverSeen: String,
    val positionFrom: String,
    val fromAddress: String,
    val fromCabinet: String,
    val fromKkm: String,
    val geoSource: String,

    val exchangeTitle: String,
    val exchangeHint: String,
    val exchangeEmpty: String,
    val exchangeEmptyHint: String,
    val exchangeNotFound: String,
    val exchangeNotFoundHint: String,
    val search: String,
    val allRegisters: String,
    val exchangeAddress: String,
    val firstSeen: String,
    val lastSeen: String,

    val sales: AnalyticsSalesTexts
)

/**
 * Надписи торговой сводки.
 *
 * Вынесены вложенным набором, а не досыпаны к прежним трём десяткам:
 * разговор здесь другой — деньги, чеки и доставка документов, — и общий
 * список из семидесяти полей перестал бы читаться.
 *
 * Названий видов расчётов тут нет намеренно: наличные, карта и мобильный
 * платёж уже названы в справочнике кассы, и второй перевод разошёлся бы
 * с тем, что напечатано на чеке. Своё здесь только «прочее» — строка,
 * которой в справочнике нет.
 */
data class AnalyticsSalesTexts(
    val tab: String,
    val forPeriod: String,

    /** Карточка, с которой начинается вкладка: главные числа сети за срок. */
    val overview: String,
    val overviewHint: String,

    /** Налог на добавленную стоимость: то самое число, ради которого смотрят сводку. */
    val vat: String,

    /** Какая часть расчётов прошла картой, электронными деньгами и мобильным платежом. */
    val cashless: String,

    /** Чем мерится изменение: прошлым сроком такой же длины. */
    val versusPrevious: String,

    /** Процентные пункты: ими меряется изменение доли, а не процентами от процента. */
    val percentPoints: String,

    /** Касс, от которых за срок пришёл хоть один чек. */
    val online: String,

    /** Касс, не пробивших за срок ни одного чека. */
    val silent: String,

    val revenue: String,
    val receipts: String,
    val average: String,
    val refunds: String,
    val tax: String,
    val net: String,

    val empty: String,
    val emptyHint: String,

    val openShifts: String,
    val offline: String,
    /** Документы, о которых известно, что они ждут ответа КГД. */
    val queuedCount: String,

    /** Документы, о доставке которых не известно ничего. */
    val unknownCount: String,

    /**
     * Деньги, выданные из кассы за скупленное у населения.
     *
     * Названия самой покупки и возврата покупки тут нет намеренно: они
     * уже заведены в наборе кабинета и стоят в журнале документов,
     * а второй перевод разошёлся бы с первым.
     */
    val paidOut: String,

    /** Почему покупка стоит в стороне от выручки. */
    val purchasesHint: String,

    val byDay: String,
    val byHour: String,
    val payments: String,
    val paymentOther: String,
    val noPayments: String,
    val nothingToDraw: String,

    val registers: String,
    val places: String,
    val allRegistersShown: String,
    val allPlacesShown: String,
    val colName: String,

    /** Свод точек по регионам: столбцы и то, чем назван регион без адреса. */
    val regions: String,
    val regionsHint: String,
    val region: String,
    val placeCount: String,
    val activeRegisters: String,
    val networkShare: String,
    val noAddress: String,

    val delivery: String,
    val delivered: String,
    val queued: String,

    /** Плитка: о доставке ничего не известно. */
    val unknown: String,
    val rejected: String,

    /**
     * Подсказки разделов сводки: что за число в разделе и что с ним делать.
     *
     * Держатся рядом с названиями самих разделов, а не отдельным блоком
     * в конце: название и объяснение правятся одной правкой, и разойтись
     * им негде.
     */
    val byDayHint: String,
    val byHourHint: String,
    val paymentsHint: String,
    val registersHint: String,
    val placesHint: String,
    val deliveryHint: String
)

/** Надписи аналитики на выбранном языке. */
fun analyticsTexts(language: Language): AnalyticsTexts = when (language) {
    Language.Kk -> analyticsTextsKk
    Language.Ru -> analyticsTextsRu
    Language.En -> analyticsTextsEn
}
