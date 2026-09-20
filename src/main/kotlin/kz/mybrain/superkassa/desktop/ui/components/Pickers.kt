package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Выбор значения из справочника узла.
 *
 * Названия ОФД, контуров и ролей приходят с узла сразу на трёх языках.
 * Свой список в приложении означал бы, что при добавлении нового ОФД
 * кассир его не увидит, а при переименовании старого прочтёт не то,
 * что напечатано в чеке.
 */
@Composable
fun DictionaryPicker(
    label: String,
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    width: Dp
) {
    LabelledPicker(
        label = label,
        options = entries,
        selected = entries.firstOrNull { it.code == selectedCode },
        title = { entry -> entry?.title(language) ?: selectedCode },
        onSelect = { onSelect(it.code) },
        available = { it.supported },
        width = width
    )
}

/** Выбор ОФД. */
@Composable
fun ProviderPicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit
) = DictionaryPicker(
    label = LocalStrings.current.settings.ofd,
    entries = entries,
    language = language,
    selectedCode = selectedCode,
    onSelect = onSelect,
    width = Sizes.fieldChoice
)

/** Выбор контура: стенд, тестовый или промышленный. */
@Composable
fun EnvironmentPicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit
) = DictionaryPicker(
    label = LocalStrings.current.settings.environment,
    entries = entries,
    language = language,
    selectedCode = selectedCode,
    onSelect = onSelect,
    width = Sizes.fieldChoice
)
