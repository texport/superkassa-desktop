package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Какая касса сейчас в работе.
 *
 * Карточка приподнята над остальными разделами: с неё кассир начинает
 * читать экран и по ней понимает, настройки какой машины перед ним.
 *
 * Своё название живёт только на этом рабочем месте: сведения о кассе
 * приходят от ОФД, и подменять их локально нельзя. Переименование нужно
 * там, где в зале три одинаковых кассы и их различают по месту, а не
 * по номеру.
 *
 * Смена кассы уводит на экран входа, а не переключается на месте: иначе
 * кассир меняет кассу, не заметив, и пробивает чек не на той машине.
 */
@Composable
internal fun CurrentKkmCard(session: Session) {
    val texts = LocalStrings.current
    val money = moneyTexts(session.language).kkm
    val kkm = session.selected ?: return
    // Поле привязано к кассе: после смены кассы в нём не должно остаться
    // название предыдущей.
    var localName by remember(kkm.kkmId) {
        mutableStateOf(session.displayName(kkm).takeIf { it != kkm.title }.orEmpty())
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = texts.settings.currentKkm,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(session.displayName(kkm), style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = { session.switchKkm() }) { Text(texts.settings.changeKkm) }
            }
            Text(
                text = whatItIs(session, kkm, texts.login.factory),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Поле и кнопки одной геометрии и выровнены по верху:
            // строка «поле — кнопка» разной высоты читается как сбой вёрстки.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight),
                itemVerticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    label = { Text(texts.settings.localName) },
                    trailingIcon = { InfoTip(texts.settings.localNameHint) },
                    singleLine = true,
                    modifier = Modifier.fieldWidth(texts.settings.localName, Sizes.fieldName)
                )
                FieldButton(texts.settings.save) {
                    session.rename(kkm, localName)
                    session.report(money.renameSaved)
                }
                FieldButton(
                    text = money.renameReset,
                    kind = FieldButtonKind.Outlined,
                    enabled = localName.isNotBlank()
                ) {
                    localName = ""
                    session.rename(kkm, null)
                    session.report(money.renameReset)
                }
            }
        }
    }
}

/** Организация, заводской номер и адрес — одной строкой под названием. */
private fun whatItIs(session: Session, kkm: Kkm, factoryLabel: String): String = listOfNotNull(
    kkm.orgTitle,
    kkm.factoryNumber?.let { "$factoryLabel $it" },
    kkm.orgAddress.takeIf { it.isNotBlank() }
).joinToString(SEPARATOR).ifBlank { session.displayName(kkm) }

private const val SEPARATOR = " · "
