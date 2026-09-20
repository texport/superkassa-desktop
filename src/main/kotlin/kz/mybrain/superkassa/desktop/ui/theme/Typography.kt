package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material3.Typography
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
 * Начертание денежных сумм.
 *
 * Цифры моноширинные и выровнены по правому краю: столбец сумм читается
 * сверху вниз одним движением глаза, а не выискивается по разной ширине
 * знаков. Это единственное место, где касса задаёт шрифт сама.
 */
object MoneyStyle {

    /** Итог чека и остаток ящика — главное число экрана. */
    val hero: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End
    )

    /** Сумма строки в списке. */
    val row: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.End
    )

    /** Сумма в подписи: цена за единицу, сдача, оборот. */
    val caption: TextStyle = TextStyle(
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
