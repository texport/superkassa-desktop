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
 * Названия контуров и ролей приходят с узла сразу на трёх языках. Свой
 * список в приложении означал бы, что при переименовании значения кассир
 * прочтёт не то, что напечатано в чеке.
 *
 * @param available принимает ли узел это значение сейчас; по умолчанию
 * так, как сказал сам узел.
 */
@Composable
fun DictionaryPicker(
    label: String,
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    width: Dp,
    available: (DictionaryEntry) -> Boolean = { it.supported }
) {
    LabelledPicker(
        label = label,
        options = entries,
        selected = entries.firstOrNull { it.code == selectedCode },
        title = { entry -> entry?.title(language) ?: selectedCode },
        onSelect = { onSelect(it.code) },
        available = available,
        width = width
    )
}

/**
 * Выбор контура: стенд, тестовый или промышленный.
 *
 * Неподнятый контур гаснет по общему правилу выпадающего списка —
 * признак готовности объявлен в [environmentRaised], а не здесь и не
 * в вызовах: разойдись он по экранам, кабинет предлагал бы контур,
 * которого мастеру нет.
 */
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
    width = Sizes.fieldChoice,
    available = { it.environmentRaised() }
)
