package kz.mybrain.superkassa.strings.api.analytics

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

    /**
     * Налог на добавленную стоимость с продаж срока.
     *
     * Назван «с продаж» намеренно: кабинет отдаёт налог пробитых продаж
     * и не вычитает из него возвраты, а рядом стоит выручка за вычетом
     * возвратов — без этого слова одно читалось бы как налог с другого.
     */
    val vat: String,

    /** Подпись под нулём налога: НДС за срок не начислялся — и почему так бывает. */
    val vatNone: String,

    /** Какая часть расчётов прошла картой, электронными деньгами и мобильным платежом. */
    val cashless: String,

    /** Чем мерится изменение: прошлым сроком такой же длины. */
    val versusPrevious: String,

    /** Процентные пункты: ими меряется изменение доли, а не процентами от процента. */
    val percentPoints: String,

    /** Касс, от которых за срок пришёл хоть один чек, — с продажами, а не «на связи». */
    val online: String,

    /**
     * Касс, не пробивших за срок ни одного чека.
     *
     * Названо фактом, а не выводом. Прежде это читалось «молчат» и было
     * покрашено отказом, то есть обещало потерянную связь или закрытую
     * точку. Сводка кабинета о причине не говорит ничего: в парке показа
     * из 3294 касс 3288 — черновики, которым КГД учёта ещё не дал
     * и которым по закону торговать нечем. Красное число о трёх тысячах
     * исправных машин посылало владельца искать поломку, которой нет.
     */
    val silent: String,

    /** Открытых смен в сети сейчас — по кассам, как в учёте. */
    val openShifts: String,

    val revenue: String,
    val receipts: String,
    val average: String,
    val refunds: String,
    val tax: String,
    val net: String,

    val empty: String,
    val emptyHint: String,

    val offline: String,

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
