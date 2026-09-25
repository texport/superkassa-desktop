package kz.mybrain.superkassa.designsystem.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.picker.WideChoiceSegments
import kz.mybrain.superkassa.designsystem.section.FactLines
import kz.mybrain.superkassa.designsystem.section.PartTitle
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons

/*
 * Общие элементы разделов по одному: карточка, строка-переключатель,
 * сегменты выбора, подпись части, строки сведений и пустое состояние.
 * Слова — образцы: у дизайн-системы своих надписей экранов нет.
 */

@ElementPreviews
@Composable
private fun SectionCardPreview() = PreviewTheme {
    SectionCard(title = "Печатная форма", info = "Как касса печатает чек") {
        PartTitle("Язык чека")
        WideChoiceSegments(listOf("Оба", "Қазақша", "Русский"), "Оба", { it }) {}
    }
}

@ElementPreviews
@Composable
private fun SwitchRowPreview() = PreviewTheme {
    SwitchRow(
        title = "Закрывать смену самой через сутки",
        checked = true,
        onSwitch = {},
        hint = "Смена дольше суток запрещена"
    )
}

@ElementPreviews
@Composable
private fun SwitchRowOffPreview() = PreviewTheme {
    SwitchRow(title = "Реклама ОФД на чеке", checked = false, onSwitch = {}, enabled = false)
}

@ElementPreviews
@Composable
private fun FactLinesPreview() = PreviewTheme {
    FactLines("Сведения о кассе", listOf("Версия" to "1.0.6", "Хранилище" to "На этой машине (SQLite)"), "Нет сведений")
}

@ElementPreviews
@Composable
private fun EmptyStatePreview() = PreviewTheme {
    EmptyState(AppIcons.noDocuments, "Документов нет", "Пробейте чек — он появится здесь")
}
