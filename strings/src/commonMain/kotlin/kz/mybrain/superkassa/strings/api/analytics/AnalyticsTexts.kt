package kz.mybrain.superkassa.strings.api.analytics

/**
 * Надписи раздела «Аналитика» личного кабинета.
 *
 * Раздел отвечает на четыре вопроса владельца сети: где стоят кассы,
 * откуда они выходят на связь, чем торгуют и как ведётся их учёт. Разговор здесь свой —
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
    val signedOut: String,
    val signedOutHint: String,
    val refused: String,

    val placed: String,

    /**
     * Касс, чей адрес карта ещё ищет.
     *
     * Счётчик свой, а не в числе оставшихся без положения: адрес у такой
     * кассы есть, и через минуту она встанет на карту сама. Сведённые
     * в одно число, они обещали бы владельцу три тысячи касс без адреса
     * там, где адрес есть у всех.
     */
    val searchingCount: String,
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

    /** Плитки не приехали: кассы на карте стоят, а подложки под ними нет. */
    val mapNoTiles: String,

    /**
     * Итог по видимому куску карты.
     *
     * Сеть в две тысячи касс не сосчитать глазами по кружкам, и владелец
     * должен знать, всю ли её он сейчас видит. Вторая строка — про отбор,
     * и без действующего отбора её нет.
     */
    val mapShown: String,
    val mapShownOf: String,

    /**
     * Сколько из видимых касс стоит на учёте КГД.
     *
     * Первое, о чём спрашивают карту сети: остальные числа говорят,
     * сколько касс заведено, и ни одно — сколько из них работает по закону.
     */
    val mapOnRecordOf: String,
    val mapSievedOf: String,

    /** Легенда карты: что значат цвет и размер кружка. */
    val mapLegend: String,
    val legendGood: String,
    val legendIdle: String,
    val legendSomeTrouble: String,
    val legendTrouble: String,
    val legendSize: String,
    val legendChosen: String,

    val searchKkm: String,
    val searchKkmLabel: String,
    val allPlaces: String,

    /** Поиск в меню торговых точек — их у сети тысячи. */
    val placeSearch: String,

    /** Подходящих точек больше, чем показано: `%1$s` — сколько показано. */
    val placesMore: String,

    /**
     * Слова отбора: учёт КГД, признаки кассы, пустой результат и сброс.
     *
     * Свой набор здесь не заводится: те же слова спрашивает колонка
     * торговых точек кабинета, и разойтись им нельзя — состав объявлен
     * в [SieveTexts] и берётся оттуда обоими разделами.
     */
    val sieve: SieveTexts,
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
    /** Подпись поля поиска адресов обмена; чем искать — примером в самом поле ([search]). */
    val searchLabel: String,
    val search: String,
    val allRegisters: String,
    val exchangeAddress: String,
    val firstSeen: String,
    val lastSeen: String,

    val sales: AnalyticsSalesTexts,

    /** Учёт касс: свой набор, состав задан в [AnalyticsRecordTexts]. */
    val record: AnalyticsRecordTexts
)
