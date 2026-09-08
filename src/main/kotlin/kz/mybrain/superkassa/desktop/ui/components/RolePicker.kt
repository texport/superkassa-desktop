package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/** Выбор роли кассира. Названия ролей — из справочника узла. */
@Composable
fun RolePicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit
) = DictionaryPicker(
    label = LocalStrings.current.users.role,
    entries = entries,
    language = language,
    selectedCode = selectedCode,
    onSelect = onSelect,
    width = Sizes.fieldAmount
)
