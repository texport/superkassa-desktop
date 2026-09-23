package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.adaptive.TableWidths
import kz.mybrain.superkassa.desktop.ui.theme.LocalTextScale

/**
 * Столбцы таблицы, растущие вместе с выбранным размером шрифта.
 *
 * Наименьшие ширины `TableColumns`
 * рассчитаны на обычную ступень. На крупной ступени сумма в миллиарды
 * даже самой малой ступенью занимает 167 точек против 134 в столбце
 * и наезжала на соседний, а «Қабылданбады» рвалось посреди слова.
 * Столбец с текстом растёт во столько же раз, во сколько вырос шрифт;
 * мельче обычной ступени — остаётся своей наименьшей ширины. Кнопка
 * от шрифта не зависит: её 48 точек заданы Material 3.
 *
 * @param texts столбцы с надписями и числами.
 * @param buttons номера столбцов с кнопками — они не растут.
 */
@Composable
internal fun grownColumns(texts: List<TableColumn>, buttons: Set<Int> = emptySet()): List<TableColumn> {
    val factor = LocalTextScale.current.factor.coerceAtLeast(1f)
    return remember(texts, buttons, factor) {
        texts.mapIndexed { at, column -> if (at in buttons) column else column.copy(min = column.min * factor) }
    }
}

/** Ширины вне таблицы: строка, нарисованная сама по себе, берёт наименьшие. */
internal fun leastWidths(columns: List<TableColumn>): TableWidths = TableWidths(columns, columns.map { it.min })
