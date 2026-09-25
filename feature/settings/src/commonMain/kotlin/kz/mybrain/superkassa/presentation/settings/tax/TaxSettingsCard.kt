package kz.mybrain.superkassa.presentation.settings.tax

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.designsystem.list.ListRows
import kz.mybrain.superkassa.designsystem.picker.LabelledPicker
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.settings.SettingRequirements
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Налоговый режим кассы, её ставка по умолчанию, автозакрытие и автоизъятие.
 *
 * Режим определяет, чем облагаются чеки этой кассы, а ставка по умолчанию —
 * с какой начинается каждая новая позиция. У плательщика НДС неверная
 * ставка по умолчанию занижает налог в каждом чеке, набранном руками.
 */
@Composable
fun TaxSettingsCard(tax: TaxSettingsUiState, actions: TaxSettingsActions) {
    val texts = LocalStrings.current
    tax.kkm ?: return
    SettingGroup(title = texts.settingsScreen.taxSettings, info = texts.settingsScreen.taxSettingsHint) {
        TaxFields(tax, actions)
        val core = textsOf(LocalLanguage.current).settings.core
        // Переключатель и есть действие: уходит в кассу сразу. Касса меняет
        // его только в режиме программирования; вне режима он погашен,
        // а не отвечает отказом на каждое нажатие.
        ListRows {
            SwitchRow(
                title = texts.settingsScreen.autoCashout,
                checked = tax.kkm.autoCashout,
                onSwitch = actions::switchAutoCashout,
                enabled = tax.switchable,
                hint = texts.settingsScreen.autoCashoutHint
            )
            SwitchRow(
                core.autoClose,
                tax.kkm.autoCloseShift,
                actions::switchAutoClose,
                tax.switchable,
                core.autoCloseHint
            )
        }
    }
}

/**
 * Режим и ставка по умолчанию — из справочников кассы.
 *
 * Пока справочники не прочитаны, на их месте стоит отказ с повтором,
 * а не два пустых поля: владелец видел бы рамки без значений и не понял бы,
 * у кого ничего нет и что с этим делать.
 */
@Composable
private fun ColumnScope.TaxFields(tax: TaxSettingsUiState, actions: TaxSettingsActions) {
    val texts = LocalStrings.current.settingsScreen
    val trouble =
        ScreenState.Trouble(texts.dictionariesMissing, texts.dictionariesMissingHint, actions::retryDictionaries)
    ScreenSlot(if (tax.dictionariesMissing) trouble else ScreenState.Ready, dense = true) {
        Picker(texts.taxRegime, tax.regimes.map { it.code to it.name }, tax.regime, actions::chooseRegime)
        if (tax.vatChoosable) {
            Picker(texts.defaultVatGroup, tax.vatRates.map { it.code to it.name }, tax.vatGroup, actions::chooseVat)
        }
        SettingRequirements(tax.needs, textsOf(LocalLanguage.current).kassa.money.kkm)
        Button(enabled = tax.savable, onClick = actions::saveTax) { Text(texts.save) }
    }
}

/** Выбор значения справочника кассы: коды на экране кассы недопустимы. */
@Composable
private fun Picker(
    label: String,
    entries: List<Pair<String, TrilingualMessageResponse>>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val language = LocalLanguage.current
    LabelledPicker(
        label = label,
        options = entries,
        selected = entries.firstOrNull { it.first == selected },
        title = { entry -> entry?.second?.of(language) ?: selected.orEmpty() },
        onSelect = { onSelect(it.first) }
    )
}
