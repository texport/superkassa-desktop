package kz.mybrain.superkassa.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.list.SectionListItem
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.PaneTitle
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons

/*
 * Части «списка и подробностей» по одной: строка списка разделов,
 * заголовок панели со стрелкой назад и без неё, группа настроек и группа
 * необратимого. Слова — образцы: у дизайн-системы своих надписей экранов нет.
 */

@ElementPreviews
@Composable
private fun SectionListItemPreview() = PreviewTheme {
    Column {
        SectionListItem(AppIcons.sectionKkm, "Касса", "Касса у входа", selected = true) {}
        SectionListItem(AppIcons.sectionPrinting, "Печать", "Форма чека и принтер", selected = false) {}
    }
}

@ElementPreviews
@Composable
private fun PaneTitlePreview() = PreviewTheme {
    Column {
        PaneTitle("Настройки")
        PaneTitle("Печать", onBack = {}, backLabel = "Назад")
    }
}

@ElementPreviews
@Composable
private fun SettingGroupPreview() = PreviewTheme {
    SettingGroup(
        title = "Режим программирования",
        info = "Касса принимает настройки только в этом режиме",
        trailing = { TextButton(onClick = {}) { Text("Сменить кассу") } }
    ) {
        SwitchRow(title = "Войти в программирование", checked = false, onSwitch = {})
    }
}

@ElementPreviews
@Composable
private fun DangerGroupPreview() = PreviewTheme {
    SettingGroup(title = "Снять кассу с учёта", danger = true) {
        Text("Касса уйдёт с рабочего места вместе с журналом")
    }
}
