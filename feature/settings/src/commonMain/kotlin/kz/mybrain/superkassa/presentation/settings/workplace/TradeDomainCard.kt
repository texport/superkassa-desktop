package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.LabelledPicker
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.words.kassa.label
import kz.mybrain.superkassa.presentation.words.kassa.title
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Отрасль, в которой работает эта касса.
 *
 * Протокол требует вид отрасли у каждого чека, но отрасль у кассы одна:
 * на заправке не бывает чеков стоянки, а в магазине — чеков такси.
 * Прежде выбор стоял на экране продажи, и кассир магазина в каждом чеке
 * видел шесть отраслей и обязательные для выбранной поля — ни одного
 * из них он не заполняет, а незаполненное держало кнопку погашенной.
 *
 * Выбор запоминается на рабочем месте и действует сразу: касса кланяется
 * ему на следующем же чеке. Своей настройки отрасли у кассы нет — вид
 * отрасли уходит в неё с каждым чеком, — поэтому помнит его рабочее место.
 *
 * Под выбором названы поля, которые кассир увидит на продаже: владелец
 * меняет настройку, зная, чего она потребует от кассира. У торговли
 * полей нет, и строки под выбором тоже.
 */
@Composable
fun TradeDomainCard(workplace: WorkplaceSettingsUiState, actions: WorkplaceSettingsActions) {
    val texts = LocalStrings.current
    workplace.kkmId ?: return
    val kind = DomainKind.byCode(workplace.domainCode)
    SectionCard(title = texts.settingsScreen.tradeDomain, info = texts.settingsScreen.tradeDomainHint) {
        LabelledPicker(
            label = texts.settingsScreen.domainKind,
            options = DomainKind.entries,
            selected = kind,
            title = { it?.title(texts.enums).orEmpty() },
            onSelect = { actions.chooseDomain(it.code) }
        )
        RequisiteNames(kind)
    }
}

/** Чем обернётся выбор на экране кассира: подписи тех самых полей. */
@Composable
private fun RequisiteNames(kind: DomainKind) {
    val texts = LocalStrings.current
    val sale = textsOf(LocalLanguage.current).kassa.sale
    if (kind.fields.isEmpty()) return
    Text(
        text = "${texts.settingsScreen.domainFields}: ${kind.fields.joinToString { it.label(sale) }}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
