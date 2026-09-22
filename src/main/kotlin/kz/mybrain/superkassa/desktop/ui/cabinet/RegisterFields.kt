package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.SearchablePicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language

/** Поля заводимой кассы. */
@Composable
internal fun RegisterFields(
    texts: CabinetTexts,
    language: Language,
    draft: RegisterDraft,
    places: List<RetailPlace>,
    models: List<KkmModel>,
    stamped: Boolean,
    issued: Boolean,
    onCreatePlace: () -> Unit
) {
    // Точка заводится отсюда же: у владельца без торговых точек форма
    // просила выбрать точку и не давала её создать — мастер подключения
    // кассы упирался в тупик на первом же шаге.
    PlacePicker(
        label = texts.place,
        texts = texts,
        language = language,
        places = places,
        selected = draft.place,
        onSelect = { draft.place = it },
        onCreate = onCreatePlace
    )
    // Модель — поиском, а не перебором: в справочнике ИСНА их сотни,
    // и владелец набирает то, что помнит, — часть названия или код.
    SearchablePicker(
        label = texts.model,
        options = models,
        selected = draft.model,
        title = { it.name ?: it.modelCode },
        keys = { listOfNotNull(it.name, it.modelCode) },
        notFound = texts.modelNotFound,
        onSelect = { draft.model = it }
    )
    if (!stamped) {
        // Выданные узлом номер и год показаны погашенными: они уже
        // присвоены кассе, и правка сделала бы их неправдой.
        FormField(
            label = texts.factoryNumber,
            value = draft.factory,
            hint = texts.factoryIssued.takeIf { issued },
            enabled = !issued
        ) { draft.factory = it }
        FormField(
            label = texts.manufactureYear,
            value = draft.year,
            hint = texts.factoryIssued.takeIf { issued },
            enabled = !issued
        ) { draft.year = it.filter(Char::isDigit).take(YEAR_DIGITS) }
    }
    FormField(texts.internalName, draft.name) { draft.name = it }
}

/** Поле формы во всю ширину карточки с подписью под ним. */
@Composable
private fun FormField(
    label: String,
    value: String,
    hint: String? = null,
    enabled: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}
