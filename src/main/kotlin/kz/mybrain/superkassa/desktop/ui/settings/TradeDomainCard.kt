package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts

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
 * ему на следующем же чеке, узла настройка не касается — ему вид отрасли
 * приходит с чеком.
 *
 * Под выбором названы поля, которые кассир увидит на продаже: владелец
 * меняет настройку, зная, чего она потребует от кассира. У торговли
 * полей нет, и строки под выбором тоже.
 */
@Composable
internal fun TradeDomainCard(session: Session) {
    val texts = LocalStrings.current
    val kkm = session.selected ?: return
    SectionCard(title = texts.settings.tradeDomain, info = texts.settings.tradeDomainHint) {
        LabelledPicker(
            label = texts.settings.domainKind,
            options = DomainKind.entries,
            selected = session.domain,
            title = { kind -> kind?.title(texts.enums).orEmpty() },
            onSelect = { session.chooseDomain(kkm.kkmId, it) }
        )
        RequisiteNames(session)
    }
}

/** Чем обернётся выбор на экране кассира: подписи тех самых полей. */
@Composable
private fun RequisiteNames(session: Session) {
    val texts = LocalStrings.current
    val sale = saleTexts(session.language)
    val fields = session.domain.fields
    if (fields.isEmpty()) return
    Text(
        text = "${texts.settings.domainFields}: ${fields.joinToString { it.label(sale) }}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
