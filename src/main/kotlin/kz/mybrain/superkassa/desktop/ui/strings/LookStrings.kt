package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи выбора тона, шрифта и размера в карточке «Оформление».
 *
 * Свой набор внутри настроек: карточка одна, а надписей у неё два
 * десятка, и в общем перечне настроек они заслонили бы всё остальное.
 */
data class LookStrings(
    val accent: String,
    val accentHint: String,
    val accentIndigo: String,
    val accentBlue: String,
    val accentTeal: String,
    val accentGreen: String,
    val accentAmber: String,
    val accentOrange: String,
    val accentRed: String,
    val accentViolet: String,
    val typeface: String,
    val typefaceSystem: String,
    val typefaceSans: String,
    val typefaceSerif: String,
    val typefaceMono: String,
    val textScale: String,
    val textScaleCompact: String,
    val textScaleNormal: String,
    val textScaleLarge: String
)

internal val russianLook = LookStrings(
    accent = "Тон",
    accentHint = "Основной цвет кнопок, выделения и значков. Отказ остаётся красным при любом тоне.",
    accentIndigo = "Индиго",
    accentBlue = "Синий",
    accentTeal = "Бирюзовый",
    accentGreen = "Зелёный",
    accentAmber = "Янтарный",
    accentOrange = "Оранжевый",
    accentRed = "Красный",
    accentViolet = "Фиолетовый",
    typeface = "Шрифт",
    typefaceSystem = "Системный",
    typefaceSans = "Без засечек",
    typefaceSerif = "С засечками",
    typefaceMono = "Моноширинный",
    textScale = "Размер",
    textScaleCompact = "Компактный",
    textScaleNormal = "Обычный",
    textScaleLarge = "Крупный"
)

internal val kazakhLook = LookStrings(
    accent = "Реңк",
    accentHint = "Түймелердің, ерекшелеудің және белгішелердің негізгі түсі. Бас тарту кез келген реңкте қызыл болып қалады.",
    accentIndigo = "Индиго",
    accentBlue = "Көк",
    accentTeal = "Көгілдір",
    accentGreen = "Жасыл",
    accentAmber = "Кәріптас",
    accentOrange = "Қызғылт сары",
    accentRed = "Қызыл",
    accentViolet = "Күлгін",
    typeface = "Қаріп",
    typefaceSystem = "Жүйелік",
    typefaceSans = "Кертіксіз",
    typefaceSerif = "Кертікті",
    typefaceMono = "Бір енді",
    textScale = "Өлшем",
    textScaleCompact = "Ықшам",
    textScaleNormal = "Қалыпты",
    textScaleLarge = "Үлкен"
)

internal val englishLook = LookStrings(
    accent = "Accent",
    accentHint = "Main colour of buttons, selection and icons. Refusal stays red with any accent.",
    accentIndigo = "Indigo",
    accentBlue = "Blue",
    accentTeal = "Teal",
    accentGreen = "Green",
    accentAmber = "Amber",
    accentOrange = "Orange",
    accentRed = "Red",
    accentViolet = "Violet",
    typeface = "Font",
    typefaceSystem = "System",
    typefaceSans = "Sans-serif",
    typefaceSerif = "Serif",
    typefaceMono = "Monospace",
    textScale = "Size",
    textScaleCompact = "Compact",
    textScaleNormal = "Regular",
    textScaleLarge = "Large"
)
