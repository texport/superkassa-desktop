package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.documentDetails
import kz.mybrain.superkassa.desktop.ui.adaptive.MoneyText
import kz.mybrain.superkassa.desktop.ui.adaptive.WrapRow
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ofdRefusalWords
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors
import kz.mybrain.superkassa.desktop.ui.theme.TableColumns

/**
 * Документы смены, которые ОФД отверг.
 *
 * В счётчики они не идут и фискальными не считаются — значит, в общем
 * списке документов они выглядят такими же строками, как принятые чеки,
 * и разобраться, что пошло не так, нечем. Здесь их число, причина отказа
 * и кто оформил: с этим уже можно идти к обслуживанию.
 *
 * Пока отказов нет, карточки нет вовсе: пустая строка «ошибок: 0» —
 * шум на главном экране.
 */
@Composable
internal fun RefusedDocuments(session: Session) {
    val texts = LocalStrings.current
    val refused = session.documents.filterNot { it.printable }
    if (refused.isEmpty()) return
    val operators = remember { mutableStateMapOf<String, String>() }

    // Кто оформил, узел отдаёт вместе с составом документа: в списке
    // документов этого поля нет, а без имени отказ — «кто-то в 14:53».
    LaunchedEffect(refused.map { it.id }) {
        val kkm = session.selected ?: return@LaunchedEffect
        refused.forEach { document ->
            if (operators.containsKey(document.id)) return@forEach
            val details = session.guard(texts.dashboard.refused) {
                session.client.documentDetails(kkm.kkmId, document.id, session.pin)
            }
            details?.operatorName?.let { operators[document.id] = it }
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${texts.dashboard.refused}: ${refused.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = StatusColors.refused
                )
                InfoTip(texts.dashboard.refusedHint)
            }
            // Отказы приходят пачкой, и перечень прокручивается внутри
            // карточки: за сотню строк он выдавливал за нижний край окна
            // и заголовок «Документы смены», и сам список. Высоту карточке
            // отмеряет раскладка главного экрана — рядом со списком смены
            // или под ним, но не больше его доли.
            ScrollableColumn(
                modifier = Modifier.weight(1f, fill = false),
                spacing = Spacing.tight,
                gutter = Spacing.snug
            ) {
                refused.forEach { document ->
                    RefusedRow(session, document, operators[document.id])
                }
            }
        }
    }
}

/** Строка отказа: что за документ, на какую сумму, чей и по какой причине. */
@Composable
private fun RefusedRow(session: Session, document: Document, operator: String?) {
    val texts = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        // Сумма и код переносятся под вид документа, когда карточке тесно,
        // а не сжимают его до многоточия.
        WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
            Text(
                text = listOfNotNull(
                    session.titleOf(Dictionary.DocumentTypes, document.docType),
                    operator
                ).joinToString(Glyphs.SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).widthIn(min = TableColumns.name)
            )
            MoneyText(Money.formatTiyn(document.totalAmount))
            // Код показывается, только когда он есть: у протокольного отказа
            // ОФД своего кода не присылает, и строка «Код отказа —» ничего
            // кассиру не сообщала.
            document.refusalCode?.let { code ->
                Text(
                    text = "${texts.common.refusalCode} $code",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusColors.refused
                )
            }
        }
        // Причина — словами кассира по коду отказа, а пояснение БФД
        // приходит по-английски и остаётся для незнакомых кодов: кассир
        // стоит перед покупателем и решает, что делать с чеком, а
        // «Same customer and taxpayer IIN» ему в этом не помогает.
        val words = ofdRefusalWords(document.refusalCode, session.language)
            ?: document.ofdErrorText
        words?.takeIf { it.isNotBlank() }?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
