package kz.mybrain.superkassa.presentation.dashboard

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.document.refusalCode
import kz.mybrain.superkassa.presentation.adaptive.MoneyText
import kz.mybrain.superkassa.presentation.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.components.InfoTip
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.components.ScrollableColumn
import kz.mybrain.superkassa.presentation.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.ofdRefusalWords
import kz.mybrain.superkassa.presentation.theme.Glyphs
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.TableColumns

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
internal fun RefusedDocuments(state: DashboardUiState) {
    // Кто оформил, касса отдаёт вместе с составом документа: в списке
    // документов этого поля нет, и модель экрана дочитывает имена сама.
    val refused = state.refused
    if (refused.isEmpty()) return

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            RefusedHead(refused.size)
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
                    RefusedRow(state, document, state.operators[document.id])
                }
            }
        }
    }
}

/** Заголовок карточки отказов: их число и что это значит. */
@Composable
private fun RefusedHead(count: Int) {
    val texts = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${texts.dashboard.refused}: $count",
            style = MaterialTheme.typography.titleMedium,
            color = StatusColors.refused
        )
        InfoTip(texts.dashboard.refusedHint)
    }
}

/** Строка отказа: что за документ, на какую сумму, чей и по какой причине. */
@Composable
private fun RefusedRow(state: DashboardUiState, document: FiscalDocumentResponse, operator: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        // Сумма и код переносятся под вид документа, когда карточке тесно,
        // а не сжимают его до многоточия.
        WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
            Text(
                text = listOfNotNull(documentTitle(state, document.docType), operator).joinToString(Glyphs.SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).widthIn(min = TableColumns.name)
            )
            MoneyText(Money.formatTiyn(document.totalAmount))
            RefusalCode(document)
        }
        RefusalReason(document)
    }
}

/**
 * Код отказа — только когда он есть: у протокольного отказа ОФД своего кода
 * не присылает, и строка «Код отказа —» ничего кассиру не сообщала.
 */
@Composable
private fun RefusalCode(document: FiscalDocumentResponse) {
    val code = document.refusalCode ?: return
    Text(
        text = "${LocalStrings.current.common.refusalCode} $code",
        style = MaterialTheme.typography.labelMedium,
        color = StatusColors.refused
    )
}

/**
 * Причина — словами кассира по коду отказа, а пояснение БФД приходит
 * по-английски и остаётся для незнакомых кодов: кассир стоит перед
 * покупателем, и «Same customer and taxpayer IIN» ему не помогает.
 */
@Composable
private fun RefusalReason(document: FiscalDocumentResponse) {
    val words = ofdRefusalWords(document.refusalCode, LocalLanguage.current) ?: document.ofdErrorText
    val reason = words?.takeIf { it.isNotBlank() } ?: return
    Text(text = reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
