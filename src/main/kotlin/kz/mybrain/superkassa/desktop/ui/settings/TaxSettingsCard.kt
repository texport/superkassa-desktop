package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.TaxSettings
import kz.mybrain.superkassa.desktop.server.updateAutoCloseShift
import kz.mybrain.superkassa.desktop.server.updateTaxSettings
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.sale.NO_VAT
import kz.mybrain.superkassa.desktop.ui.sale.NO_VAT_REGIME
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Налоговый режим кассы, её ставка по умолчанию и автозакрытие смены.
 *
 * Режим определяет, чем облагаются чеки этой кассы, а ставка по умолчанию —
 * с какой начинается каждая новая позиция. У плательщика НДС неверная
 * ставка по умолчанию занижает налог в каждом чеке, набранном руками.
 *
 * Узел принимает эти настройки только в режиме программирования, при
 * закрытой смене и пустой очереди отправки: сменить режим посреди смены
 * значит получить в одном Z-отчёте чеки с разными налогами.
 */
@Composable
fun TaxSettingsCard(session: Session) {
    val texts = LocalStrings.current
    val kkm = session.selected ?: return
    val regimes = session.dictionaries[Dictionary.TaxRegimes].orEmpty()
    val groups = session.dictionaries[Dictionary.VatGroups].orEmpty()
    var regime by remember(kkm.taxRegime) { mutableStateOf(kkm.taxRegime) }
    var group by remember(kkm.defaultVatGroup) { mutableStateOf(kkm.defaultVatGroup) }

    SectionCard(title = texts.settings.taxSettings) {
        EntryPicker(session, texts.settings.taxRegime, regimes, Dictionary.TaxRegimes, regime) {
            regime = it
            if (it == NO_VAT_REGIME) group = NO_VAT
        }
        // Ставка по умолчанию следует за режимом: у неплательщика НДС
        // выбирать не из чего, и пара NO_VAT + VAT_12 в настройках
        // упирается в отказ узла на первом же чеке.
        if (regime != NO_VAT_REGIME) {
            EntryPicker(session, texts.settings.defaultVatGroup, groups, Dictionary.VatGroups, group) {
                group = it
            }
        }
        AutoCloseRow(session, kkm)
        SaveTax(session, kkm, regime, group)
    }
}

/** Выбор значения справочника узла: коды на экране кассы недопустимы. */
@Composable
private fun EntryPicker(
    session: Session,
    label: String,
    entries: List<DictionaryEntry>,
    dictionary: Dictionary,
    selected: String?,
    onSelect: (String) -> Unit
) {
    LabelledPicker(
        label = label,
        options = entries,
        selected = entries.firstOrNull { it.code == selected },
        title = { entry -> entry?.let { session.titleOf(dictionary, it.code) }.orEmpty() },
        onSelect = { onSelect(it.code) }
    )
}

/**
 * Автозакрытие смены.
 *
 * Смена по правилам КГД живёт сутки; узел умеет закрывать её сам. Отдельной
 * кнопки у переключателя нет: он и есть действие, и уходит на узел сразу.
 */
@Composable
private fun AutoCloseRow(session: Session, kkm: Kkm) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = kkm.autoCloseShift,
            enabled = !session.busy && kkm.isProgramming,
            onCheckedChange = { wanted ->
                scope.launch {
                    session.guard(texts.settings.autoCloseShift) {
                        session.client.updateAutoCloseShift(kkm.kkmId, wanted, session.pin)
                    } ?: return@launch
                    session.refreshKkms()
                    session.report(texts.settings.settingsSaved)
                }
            }
        )
        Text(texts.settings.autoCloseShift, style = MaterialTheme.typography.bodyMedium)
        InfoTip(texts.settings.autoCloseShiftHint)
    }
}

/** Сохранение налоговых настроек: обе меняются одним обращением. */
@Composable
private fun SaveTax(session: Session, kkm: Kkm, regime: String?, group: String?) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val changed = regime != kkm.taxRegime || group != kkm.defaultVatGroup
    val ready = regime != null && group != null && changed && kkm.isProgramming
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            enabled = ready && !session.busy,
            onClick = {
                scope.launch {
                    val settings = TaxSettings(regime.orEmpty(), group.orEmpty())
                    session.guard(texts.settings.taxSettings) {
                        session.client.updateTaxSettings(kkm.kkmId, settings, session.pin)
                    } ?: return@launch
                    session.refreshKkms()
                    session.report(texts.settings.settingsSaved)
                }
            }
        ) { Text(texts.settings.save) }
        if (!kkm.isProgramming) InfoTip(texts.settings.programmingRequired)
    }
}
