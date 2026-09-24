package kz.mybrain.superkassa.designsystem.theme.size

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kz.mybrain.superkassa.designsystem.theme.type.AppTypography
import kz.mybrain.superkassa.designsystem.theme.type.LocalTextScale
import kz.mybrain.superkassa.designsystem.theme.type.scaled

/**
 * Наименьшие ширины столбцов таблиц.
 *
 * Уже этого столбец не сжимается: таблице, которой не хватает окна,
 * достаётся прокрутка вбок, а не слово столбиком по букве и не кнопка,
 * пропавшая до точки. Столбец одного смысла одной ширины во всех таблицах —
 * журнал, аналитика и кабинет стоят рядом, и суммы в них читаются одинаково.
 */
object TableColumns {

    /** Название товара, точки, кассы: растёт сильнее остальных. */
    val name = 50.steps

    /** Сумма до сотен миллионов тенге основной ступенью; больше — уменьшенной. */
    val money = 150.fine

    /** Счёт штук: чеки, кассы, позиции. */
    val count = 22.steps

    /** Номер документа, смены, номер КГД. */
    val number = 30.steps

    /** Дата и время. */
    val moment = 150.fine

    /** Одна кнопка-значок в конце строки: печать, раскрыть. Кнопка 48 и поля ячейки. */
    val action = 16.steps

    /** Поле ячейки слева и справа: столбцы не слипаются. */
    val cellPadding = Spacing.itemGap
}

/**
 * Начертание чисел в таблицах.
 *
 * Номера и счёт — моноширинно и вправо, как суммы: столбец цифр читается
 * сверху вниз одним движением, а пропорциональные цифры уводят разряды
 * вбок. Размер — как у строки списка, чтобы строка таблицы не набиралась
 * двумя кеглями. Растёт вместе с выбранным размером, как и суммы.
 */
object NumberStyle {

    /** Число в ячейке таблицы; растёт вместе с выбранным размером шрифта. */
    val cell: TextStyle
        @Composable get() = cellBase.scaled(LocalTextScale.current)

    private val cellBase = TextStyle(
        fontFamily = FontFamily.Monospace,
        // Кегль и интерлиньяж — основного текста шкалы: число в ячейке
        // читается в строку с названием рядом, и своих размеров у него нет.
        fontSize = AppTypography.bodyMedium.fontSize,
        lineHeight = AppTypography.bodyMedium.lineHeight,
        textAlign = TextAlign.End
    )
}

/**
 * Ступени суммы, которой не хватает места.
 *
 * Сумма не обрезается и не переносится посреди числа никогда: сначала
 * она берёт свою ступень, потом — меньшую на заметный глаз шаг. Шагов
 * немного и они крупные, чтобы уменьшение читалось как решение,
 * а не как дрожание кегля от строки к строке.
 */
internal object MoneyFit {

    /** Множители кегля по порядку: своя ступень, затем уменьшенные. */
    val steps: List<Float> = listOf(1f, 0.85f, 0.72f)
}
