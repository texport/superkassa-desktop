package kz.mybrain.superkassa.desktop.ui.strings

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
    val zoomIn: String,
    val zoomOut: String,
    val myLocation: String,
    val myLocationShown: String,
    val myLocationPrecise: String,
    val findHouse: String,
    val locationAsk: String,
    val locationAskHint: String,
    val locationAllow: String,
    val locationDeny: String
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

/** Надписи связки на выбранном языке. */
fun mapAddressTexts(language: Language): MapAddressTexts = when (language) {
    Language.Ru -> MapAddressTexts(
        pickAddressFirst = "Выберите адрес в регистре — карта найдёт дом; " +
            "или поставьте метку и подберите адрес по ней",
        searching = "Ищем этот адрес на карте",
        notOnMap = "Карта не нашла этот адрес — поставьте метку нажатием сами",
        byPoint = "Адрес по метке",
        byPointSearching = "Узнаём, что за место под меткой",
        byPointHouses = "Дома регистра по этой метке — выберите свой",
        byPointNoPlace = "Служба карт не узнала место под меткой — выберите адрес по шагам регистра",
        byPointNoRegion = "В регистре нет области, которую назвала карта — выберите адрес по шагам",
        byPointNoLocality = "В регистре нет населённого пункта, который назвала карта — выберите адрес по шагам",
        byPointNoStreet = "В регистре нет улицы, которую назвала карта — выберите адрес по шагам",
        byPointNoHouse = "В регистре нет дома с этим номером — выберите адрес по шагам"
    )
    Language.Kk -> MapAddressTexts(
        pickAddressFirst = "Тіркелімнен мекенжайды таңдаңыз — карта үйді табады; " +
            "немесе белгі қойып, мекенжайды сол бойынша таңдаңыз",
        searching = "Осы мекенжайды картадан іздеп жатырмыз",
        notOnMap = "Карта бұл мекенжайды таппады — белгіні картаны басып өзіңіз қойыңыз",
        byPoint = "Белгі бойынша мекенжай",
        byPointSearching = "Белгінің астында қандай орын екенін анықтап жатырмыз",
        byPointHouses = "Осы белгі бойынша тіркелім үйлері — өзіңіздікін таңдаңыз",
        byPointNoPlace = "Карта қызметі белгінің астындағы орынды танымады — мекенжайды тіркелім қадамдарымен таңдаңыз",
        byPointNoRegion = "Карта атаған облыс тіркелімде жоқ — мекенжайды қадамдармен таңдаңыз",
        byPointNoLocality = "Карта атаған елді мекен тіркелімде жоқ — мекенжайды қадамдармен таңдаңыз",
        byPointNoStreet = "Карта атаған көше тіркелімде жоқ — мекенжайды қадамдармен таңдаңыз",
        byPointNoHouse = "Тіркелімде осы нөмірлі үй жоқ — мекенжайды қадамдармен таңдаңыз"
    )
    Language.En -> MapAddressTexts(
        pickAddressFirst = "Pick the address in the registry — the map will find the building; " +
            "or place a marker and look the address up by it",
        searching = "Looking for this address on the map",
        notOnMap = "The map did not find this address — place the marker yourself by clicking",
        byPoint = "Address by marker",
        byPointSearching = "Finding out what place is under the marker",
        byPointHouses = "Registry buildings for this marker — pick yours",
        byPointNoPlace = "The map service did not recognise the place under the marker — pick the address step by step",
        byPointNoRegion = "The registry has no region the map named — pick the address step by step",
        byPointNoLocality = "The registry has no locality the map named — pick the address step by step",
        byPointNoStreet = "The registry has no street the map named — pick the address step by step",
        byPointNoHouse = "The registry has no building with this number — pick the address step by step"
    )
}
