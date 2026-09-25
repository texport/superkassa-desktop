package kz.mybrain.superkassa.designsystem.section

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Что стоит в начале шапки окна — слот `navigationIcon` Material 3.
 *
 * Одно из трёх, и никогда два сразу: стрелка назад по истории окна,
 * кнопка меню, которая на телефоне открывает все разделы, или
 * опознавательный значок там, где ни возврата, ни меню нет.
 */
sealed interface BarLead {

    /** Шаг назад по истории окна; [label] — для чтения с экрана. */
    data class Back(val onClick: () -> Unit, val label: String?) : BarLead

    /** Открыть навигацию окна; [label] — для чтения с экрана. */
    data class Menu(val onClick: () -> Unit, val label: String?) : BarLead

    /** Опознавательный значок в кружке: чей это экран. */
    data class Badge(val icon: ImageVector) : BarLead
}
