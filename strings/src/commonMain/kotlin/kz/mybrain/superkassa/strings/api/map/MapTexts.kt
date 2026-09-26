package kz.mybrain.superkassa.strings.api.map

/**
 * Надписи окна выбора точки на карте.
 *
 * Карта — свой экран со своим разговором: увеличение, своё место,
 * разрешение на его определение. В общем наборе кабинета эти полтора
 * десятка строк лежали вперемешку с заявлениями и чеками.
 */
data class MapTexts(
    val pickOnMapHint: String,
    val pickPoint: String,

    /** Поле карты пустое: плитки не доехали, но место всё равно ставится. */
    val noTiles: String,

    /**
     * Кнопка у набранных градусов.
     *
     * Своё название, а не то же, что у кнопки выбора в подвале: два
     * «Взять эти координаты» на одном окне делали разное — одна ведёт
     * карту к набранному, другая отдаёт выбранное форме точки.
     */
    val showDegrees: String,
    val zoomIn: String,
    val zoomOut: String,

    /** Раскрыть карту на весь экран. */
    val fullscreen: String,

    /** Вернуть карту в окно. */
    val fullscreenExit: String,
    val myLocation: String,
    val myLocationShown: String,
    val myLocationPrecise: String,
    val findHouse: String,
    val locationAsk: String,
    val locationAskHint: String,
    val locationAllow: String,
    val locationDeny: String,

    /** Связка карты с адресным регистром. */
    val address: MapAddressTexts
)

/**
 * Надписи связки карты с адресным регистром.
 *
 * Заведены здесь по языкам, а не в наборе кабинета: набор кабинета
 * собирается своим файлом на каждый язык, а эти строки нужны одному окну
 * карты — и меняются вместе с ним.
 */
data class MapAddressTexts(
    val pickAddressFirst: String,

    /** Метки нет, и подбирать адрес нечем: кнопка погашена не молча. */
    val markFirst: String,
    val searching: String,
    val notOnMap: String,
    val byPoint: String,
    val byPointSearching: String,
    val byPointHouses: String,
    val byPointNoPlace: String,
    val byPointNoRegion: String,
    val byPointNoLocality: String,
    val byPointNoStreet: String,
    val byPointNoHouse: String
)
