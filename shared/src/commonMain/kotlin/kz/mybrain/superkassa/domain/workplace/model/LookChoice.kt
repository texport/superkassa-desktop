package kz.mybrain.superkassa.domain.workplace.model

/**
 * Каким кассир оставил рабочее место: язык, оформление и свёрнутые части.
 *
 * Значения — коды, а не перечни оформления: что код значит на экране,
 * решает слой экранов, а рабочее место только помнит выбор. Пустой код —
 * выбора не было, и экран берёт своё умолчание.
 *
 * @property language язык интерфейса.
 * @property appearance светлая, тёмная или как в системе.
 * @property accent основной тон.
 * @property typeface шрифт.
 * @property textScale размер шрифта.
 * @property railCollapsed свёрнут ли рельс разделов.
 * @property placesCollapsed свёрнута ли колонка торговых точек кабинета.
 */
data class LookChoice(
    val language: String? = null,
    val appearance: String? = null,
    val accent: String? = null,
    val typeface: String? = null,
    val textScale: String? = null,
    val railCollapsed: Boolean = false,
    val placesCollapsed: Boolean = false
)
