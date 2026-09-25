package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи выбора тона, шрифта и размера в карточке «Оформление».
 *
 * Свой набор внутри настроек: карточка одна, а надписей у неё три
 * десятка, и в общем перечне настроек они заслонили бы всё остальное.
 */
data class LookTexts(
    /** Светлая, тёмная или как в системе. */
    val theme: String,
    val accent: String,
    val accentHint: String,
    val accentRed: String,
    val accentOrange: String,
    val accentAmber: String,
    val accentOlive: String,
    val accentLime: String,
    val accentGreen: String,
    val accentEmerald: String,
    val accentTeal: String,
    val accentAzure: String,
    val accentBlue: String,
    val accentIndigo: String,
    val accentViolet: String,
    val accentLilac: String,
    val accentPink: String,
    val typeface: String,
    val typefaceSystem: String,
    val typefaceSans: String,
    val typefaceSerif: String,
    val typefaceMono: String,
    val textScale: String,
    val textScaleHint: String,
    val textScaleDense: String,
    val textScaleCompact: String,
    val textScaleNormal: String,
    val textScaleLarge: String,
    val textScaleLarger: String
)
