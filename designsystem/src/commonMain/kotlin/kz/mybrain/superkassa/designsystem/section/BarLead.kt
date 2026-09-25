package kz.mybrain.superkassa.designsystem.section

/**
 * Что стоит в начале шапки окна — слот `navigationIcon` Material 3.
 *
 * Одно из двух, и никогда оба сразу: стрелка назад по истории окна или
 * кнопка меню, которая на телефоне открывает все разделы. Когда нет ни
 * того, ни другого, слот пуст — у всех разделов одинаково: значок
 * в кружке был только у кабинета, и шапка там выглядела чужой.
 */
sealed interface BarLead {

    /** Шаг назад по истории окна; [label] — для чтения с экрана. */
    data class Back(val onClick: () -> Unit, val label: String?) : BarLead

    /** Открыть навигацию окна; [label] — для чтения с экрана. */
    data class Menu(val onClick: () -> Unit, val label: String?) : BarLead
}
