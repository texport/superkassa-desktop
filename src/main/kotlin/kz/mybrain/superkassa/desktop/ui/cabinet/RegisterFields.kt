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
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.SearchablePicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/** Поля заводимой кассы. */
@Composable
internal fun RegisterFields(
    texts: CabinetTexts,
    draft: RegisterDraft,
    places: List<RetailPlace>,
    models: List<KkmModel>,
    stamped: Boolean,
    issued: Boolean
) {
    LabelledPicker(
        label = texts.place,
        options = places,
        selected = draft.place,
        title = { it?.name.orEmpty() },
        onSelect = { draft.place = it }
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
