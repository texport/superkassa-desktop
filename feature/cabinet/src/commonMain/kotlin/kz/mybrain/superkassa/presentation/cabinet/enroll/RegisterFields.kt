package kz.mybrain.superkassa.presentation.cabinet.enroll

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.picker.PickerWords
import kz.mybrain.superkassa.designsystem.picker.SearchablePicker
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.presentation.cabinet.component.PlaceOptions
import kz.mybrain.superkassa.presentation.cabinet.component.PlacePicker
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Поля заводимой кассы.
 *
 * @param places точки на выбор: найденные кабинетом, поиск дальше и заведение новой.
 */
@Composable
internal fun RegisterFields(
    texts: CabinetTexts,
    language: Language,
    draft: RegisterDraft,
    places: PlaceOptions,
    models: List<KkmModel>,
    stamped: Boolean,
    issued: Boolean
) {
    // Точка заводится отсюда же: у владельца без торговых точек форма
    // просила выбрать точку и не давала её создать — мастер подключения
    // кассы упирался в тупик на первом же шаге.
    PlacePicker(label = texts.place, texts = texts, language = language, options = places, selected = draft.place) {
        draft.place = it
    }
    // Модель — поиском, а не перебором: в справочнике ИСНА их сотни,
    // и владелец набирает то, что помнит, — часть названия или код.
    SearchablePicker(
        label = texts.model,
        options = models,
        selected = draft.model,
        words = PickerWords({ it.name ?: it.modelCode }, { listOfNotNull(it.name, it.modelCode) }, texts.modelNotFound),
        onSelect = { draft.model = it }
    )
    if (!stamped) FactoryFields(texts, draft, issued)
    FormField(texts.internalName, draft.name) { draft.name = it }
}

/**
 * Заводской номер и год.
 *
 * Выданные узлом номер и год показаны погашенными: они уже присвоены
 * кассе, и правка сделала бы их неправдой.
 */
@Composable
private fun FactoryFields(texts: CabinetTexts, draft: RegisterDraft, issued: Boolean) {
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
