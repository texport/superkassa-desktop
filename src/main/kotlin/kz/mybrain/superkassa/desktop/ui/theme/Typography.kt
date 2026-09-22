package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Шрифтовая шкала кассы.
 *
 * Взята шкала Material 3 и подстроена под кассовый стол: кассир читает
 * итог не с двадцати сантиметров, а с метра, поэтому крупные роли
 * чуть плотнее по начертанию, а мелкие — не мельче четырнадцати точек,
 * иначе состав чека на ленте не разобрать.
 *
 * Это базовая шкала: шрифт и размер, выбранные кассиром, накладываются
 * на неё функцией [typographyOf], а не второй шкалой с переписанными
 * числами.
 */
internal val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),

    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),

    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),

    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),

    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

/**
 * Шкала кассы под выбранный шрифт и размер.
 *
 * Каждая роль берётся из базовой и получает семейство и множитель:
 * так шкала остаётся одной, а выбор кассира — двумя числами поверх неё.
 * Высота строки растёт вместе с кеглем, иначе крупный текст слипается.
 */
internal fun typographyOf(typeface: Typeface, scale: TextScale): Typography = with(AppTypography) {
    Typography(
        displayLarge = displayLarge.styled(typeface, scale),
        displayMedium = displayMedium.styled(typeface, scale),
        displaySmall = displaySmall.styled(typeface, scale),
        headlineLarge = headlineLarge.styled(typeface, scale),
        headlineMedium = headlineMedium.styled(typeface, scale),
        headlineSmall = headlineSmall.styled(typeface, scale),
        titleLarge = titleLarge.styled(typeface, scale),
        titleMedium = titleMedium.styled(typeface, scale),
        titleSmall = titleSmall.styled(typeface, scale),
        bodyLarge = bodyLarge.styled(typeface, scale),
        bodyMedium = bodyMedium.styled(typeface, scale),
        bodySmall = bodySmall.styled(typeface, scale),
        labelLarge = labelLarge.styled(typeface, scale),
        labelMedium = labelMedium.styled(typeface, scale),
        labelSmall = labelSmall.styled(typeface, scale)
    )
}

private fun TextStyle.styled(typeface: Typeface, scale: TextScale): TextStyle =
    scaled(scale).copy(fontFamily = typeface.family)

/** Тот же стиль крупнее или мельче: кегль и высота строки в один множитель. */
internal fun TextStyle.scaled(scale: TextScale): TextStyle =
    copy(fontSize = fontSize * scale.factor, lineHeight = lineHeight * scale.factor)

/**
 * Выбранный размер шрифта — для начертаний вне шкалы Material.
 *
 * Суммы задают шрифт сами и в шкалу не входят, но расти вместе с ней
 * обязаны: кассир, выбравший крупный шрифт, выбирал его ради итога чека.
 */
val LocalTextScale = staticCompositionLocalOf { TextScale.Normal }

/**
 * Начертание денежных сумм.
 *
 * Цифры моноширинные и выровнены по правому краю: столбец сумм читается
 * сверху вниз одним движением глаза, а не выискивается по разной ширине
 * знаков. Это единственное место, где касса задаёт шрифт сама.
 *
 * Размер следует за выбранным размером шрифта кассы, см. [LocalTextScale].
 */
object MoneyStyle {

    /** Итог чека и остаток ящика — главное число экрана. */
    val hero: TextStyle
        @Composable get() = heroBase.scaled(LocalTextScale.current)

    /** Сумма строки в списке. */
    val row: TextStyle
        @Composable get() = rowBase.scaled(LocalTextScale.current)

    /** Сумма в подписи: цена за единицу, сдача, оборот. */
    val caption: TextStyle
        @Composable get() = captionBase.scaled(LocalTextScale.current)

    private val heroBase = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End
    )

    private val rowBase = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.End
    )

    private val captionBase = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.End
    )
}

/**
 * Начертание строк журнала.
 *
 * Журнал читают глазами по столбцам: время под временем, уровень под
 * уровнем. Пропорциональный шрифт сдвигает их на каждой строке, и найти
 * нужный обмен среди сотни записей становится работой. Шрифт задаётся
 * здесь, а не в окне журнала, — как и начертание сумм.
 *
 * Размеру кассы журнал не следует: это окно отладчика, а не кассира.
 */
object LogStyle {

    /** Сама строка: время, уровень, источник и текст. */
    val line: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    /** Тело запроса или ответа под строкой: его читают редко и вчитываясь. */
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}
