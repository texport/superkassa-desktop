package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Схема кассы для выбранного тона.
 *
 * Индиго — эталон: он выписан ролями руками и остаётся таким, каким
 * касса была до выбора тона. Остальные тона выводятся из своего оттенка
 * теми же правилами тональных палитр, по которым построен и индиго,
 * см. `tonalScheme`. Схемы считаются один раз: их восемь на две темы,
 * и считать пятьдесят цветов на каждый кадр незачем.
 */
internal fun schemeOf(accent: Accent, dark: Boolean): ColorScheme = Schemes.getOrPut(accent to dark) {
    when {
        accent == Accent.Indigo && dark -> DarkScheme
        accent == Accent.Indigo -> LightScheme
        else -> tonalScheme(accent.hue, dark)
    }
}

private val Schemes = mutableMapOf<Pair<Accent, Boolean>, ColorScheme>()

/**
 * Кружок цвета в выборе: заливка и отметка на ней.
 *
 * Отметка — не белая и не чёрная, а надпись на этой роли из той же
 * схемы: так она читается на любом тоне, потому что схема для того
 * и построена.
 */
class Swatch(val fill: Color, val mark: Color)

/** Каким тон показан в выборе: его главная роль в нынешней теме. */
val Accent.swatch: Swatch
    @Composable get() = schemeOf(this, LocalDarkTheme.current).let { Swatch(it.primary, it.onPrimary) }

/**
 * Тональная схема индиго по Material 3.
 *
 * Схема выписана ролями целиком, а не тремя цветами поверх стандартной:
 * Material красит поверхности, контейнеры и обводки производными от них,
 * и половинчатая схема даёт серые карточки с чужими оттенками. Индиго
 * спокойный, читается и на светлом, и на тёмном, и не спорит с зелёным
 * «доставлено» и красным «отказ».
 *
 * Роли распределены по смыслу кассы:
 * - `primary` — действие, ради которого открыт экран (пробить чек);
 * - `secondary` — спутник основного тона: тональные кнопки, выделенная
 *   строка списка, плашки выбора;
 * - `tertiary` — редкий акцент;
 * - `error` — отказ и блокировка.
 *
 * Состояния документа — «доставлено» и «ждёт» — намеренно НЕ занимают
 * вторичную и третичную роли. Material красит этими ролями каждую
 * тональную кнопку и каждый выбранный пункт, и зелёная роль «доставлено»
 * делала зелёной кнопку X-отчёта, к доставке отношения не имеющую.
 * Состояния живут отдельными цветами ниже.
 */
internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4355B9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF000F5C),
    inversePrimary = Color(0xFFBAC3FF),

    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),

    tertiary = Color(0xFF77536C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2D1226),

    error = ErrorLight.error,
    onError = ErrorLight.onError,
    errorContainer = ErrorLight.container,
    onErrorContainer = ErrorLight.onContainer,

    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),
    scrim = Color(0xFF000000)
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF11258C),
    primaryContainer = Color(0xFF2A3CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    inversePrimary = Color(0xFF4355B9),

    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFE0E1F9),

    tertiary = Color(0xFFE6BAD5),
    onTertiary = Color(0xFF45253B),
    tertiaryContainer = Color(0xFF5D3A52),
    onTertiaryContainer = Color(0xFFFFD8EE),

    error = ErrorDark.error,
    onError = ErrorDark.onError,
    errorContainer = ErrorDark.container,
    onErrorContainer = ErrorDark.onContainer,

    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A2930),
    surfaceContainerHighest = Color(0xFF35343B),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    scrim = Color(0xFF000000)
)

/**
 * Цвета состояний документа.
 *
 * Их два: «доставлено» и «ждёт отправки». Отказ берёт роль `error` схемы —
 * это и есть ошибка. Зелёный и янтарный вынесены из ролей схемы, чтобы
 * Material не красил ими тональные кнопки и выбранные строки, где никакой
 * доставки нет.
 */
internal data class StatusPalette(val delivered: Color, val pending: Color)

internal val LightStatuses = StatusPalette(
    delivered = Color(0xFF1F6C3F),
    pending = Color(0xFF7D5700)
)

internal val DarkStatuses = StatusPalette(
    delivered = Color(0xFF8CD7A2),
    pending = Color(0xFFF5BD48)
)
