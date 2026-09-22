package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.loadMissingDictionaries
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.TaxSettings
import kz.mybrain.superkassa.desktop.server.updateTaxSettings
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.sale.NO_VAT
import kz.mybrain.superkassa.desktop.ui.sale.NO_VAT_REGIME
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf

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
    SectionCard(title = texts.settings.taxSettings, info = texts.settings.taxSettingsHint) {
        TaxFields(session, kkm)
        AutoCashoutRow(session, kkm)
    }
}

/**
 * Режим и ставка по умолчанию — из справочников узла.
 *
 * Пока справочники не прочитаны, на их месте стоит отказ с повтором,
 * а не два пустых поля: узел мог молчать в тот миг, когда рабочее место
 * читало справочники при входе, и владелец видел рамки без значений,
 * а под раскрытым списком — «Выбирать не из чего», не понимая, у кого
 * ничего нет и что с этим делать.
 */
@Composable
private fun ColumnScope.TaxFields(session: Session, kkm: Kkm) {
    val texts = LocalStrings.current
    val regimes = session.dictionaries[Dictionary.TaxRegimes].orEmpty()
    val groups = session.dictionaries[Dictionary.VatGroups].orEmpty()
    // Выбранное держится черновиком: экран настроек уходит из состава
    // вместе с разделом, и владелец, заглянувший в журнал по дороге
    // к «Сохранить», возвращался к прежнему режиму.
    val regimeField = SettingsDrafts.forKkm(SettingsDrafts.Field.TAX_REGIME, kkm.kkmId)
    val groupField = SettingsDrafts.forKkm(SettingsDrafts.Field.TAX_VAT_GROUP, kkm.kkmId)
    val regime = SettingsDrafts.of(regimeField, kkm.taxRegime.orEmpty()).ifBlank { null }
    val group = SettingsDrafts.of(groupField, kkm.defaultVatGroup.orEmpty()).ifBlank { null }
    ScreenSlot(dictionariesState(session, regimes.isEmpty() || groups.isEmpty()), dense = true) {
        EntryPicker(session, texts.settings.taxRegime, regimes, Dictionary.TaxRegimes, regime) {
            SettingsDrafts.type(regimeField, it)
            if (it == NO_VAT_REGIME) SettingsDrafts.type(groupField, NO_VAT)
        }
        // Ставка по умолчанию следует за режимом: у неплательщика НДС
        // выбирать не из чего, и пара NO_VAT + VAT_12 в настройках
        // упирается в отказ узла на первом же чеке.
        if (regime != NO_VAT_REGIME) {
            EntryPicker(session, texts.settings.defaultVatGroup, groups, Dictionary.VatGroups, group) {
                SettingsDrafts.type(groupField, it)
            }
        }
        SaveTax(session, kkm, regime, group) {
            SettingsDrafts.forget(regimeField)
            SettingsDrafts.forget(groupField)
        }
    }
}

/**
 * Есть ли из чего выбирать.
 *
 * Повтор дочитывает только непришедшее: справочники читаются один раз
 * при входе, и узел, поднявшийся после него, иначе остался бы
 * неспрошенным до перезапуска приложения.
 */
@Composable
private fun dictionariesState(session: Session, missing: Boolean): ScreenState {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    if (!missing) return ScreenState.Ready
    return ScreenState.Trouble(
        title = texts.settings.dictionariesMissing,
        hint = texts.settings.dictionariesMissingHint,
        onRetry = { scope.launch { session.loadMissingDictionaries() } }
    )
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
 * Сохранение налоговых настроек: обе меняются одним обращением.
 *
 * Требования узла названы над кнопкой и гасят её. Прежде проверялся
 * только режим программирования, и при открытой смене кнопка звала узел
 * заведомо впустую: он отвечал «сначала закройте смену» — после того,
 * как владелец выбрал режим и нажал «Сохранить».
 *
 * Черновик забывается только после согласия узла: отказ оставляет
 * набранное на месте, иначе владелец правил бы его заново.
 */
@Composable
private fun SaveTax(session: Session, kkm: Kkm, regime: String?, group: String?, onSaved: () -> Unit) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val changed = regime != kkm.taxRegime || group != kkm.defaultVatGroup
    val needs = KkmSettingRules.tax(
        programming = kkm.isProgramming,
        shiftOpen = session.shiftOpen,
        queueWaiting = session.queueTasks.any { it.isWaiting }
    )
    val ready = regime != null && group != null && changed && KkmSettingRules.met(needs)
    SettingRequirements(needs, moneyTexts(session.language).kkm)
    FilledTonalButton(
        enabled = ready && !session.busy,
        onClick = {
            scope.launch {
                saveTax(session, kkm, TaxSettings(regime.orEmpty(), group.orEmpty()), onSaved)
            }
        }
    ) { Text(texts.settings.save) }
}

/** Отправляет налоговые настройки узлу и объявляет итог. */
private suspend fun saveTax(session: Session, kkm: Kkm, settings: TaxSettings, onSaved: () -> Unit) {
    val texts = stringsOf(session.language).settings
    session.guard(texts.taxSettings) {
        session.client.updateTaxSettings(kkm.kkmId, settings, session.pin)
    } ?: return
    onSaved()
    session.refreshKkms()
    session.report(texts.settingsSaved)
}
