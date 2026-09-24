package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.domain.settings.model.OfdSummary
import kz.mybrain.superkassa.presentation.common.section.FactLines
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.KkmSetupTexts
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Диагностика кассы: связь с ОФД и сведения о кассе у него.
 *
 * Режим программирования отсюда убран: выход из него стоял здесь, а вход —
 * двумя разделами выше, и кассир, вошедший в режим, искал выход по всему
 * экрану. И то и другое живёт теперь под самой кассой, в [ProgrammingCard].
 *
 * Настройки самой кассы — режим, протокол, хранилище — показывает карточка
 * «Касса на этой машине»: они не о связи, а о том, как касса работает.
 */
@Composable
fun DiagnosticsCard(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val texts = LocalStrings.current
    val money = moneyTexts(LocalLanguage.current).kkm
    ofd.kkm ?: return
    SectionCard(
        title = texts.settings.diagnostics,
        info = money.diagnosticsHint,
        // Ответ ОФД — не состояние кассы, а итог только что нажатой
        // проверки: он и остаётся здесь, в строке заголовка.
        trailing = { ofd.linkAlive?.let { LinkChip(it) } }
    ) {
        Checks(ofd.busy, actions)
        val summary = ofd.summary
        if (summary == null) {
            Text(
                text = money.diagnosticsEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FactLines(texts.settings.ofdInfo, summary.rows(money), money.ofdEmpty)
        }
    }
}

/** Проверки: связь с ОФД и его сведения о кассе. Ничего в кассе не меняют. */
@Composable
private fun Checks(busy: Boolean, actions: OfdSettingsActions) {
    val texts = LocalStrings.current.settings
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(enabled = !busy, onClick = actions::checkLink) { Text(texts.checkOfdLink) }
        OutlinedButton(enabled = !busy, onClick = actions::askInfo) { Text(texts.ofdInfo) }
    }
}

/** Ответил ли ОФД на проверку связи. */
@Composable
private fun LinkChip(alive: Boolean) {
    val texts = LocalStrings.current.settings
    if (alive) Chip(texts.ofdAnswers, StatusColors.delivered) else Chip(texts.ofdSilent, StatusColors.refused)
}

/** Сведения ОФД подписанными строками; пустые поля места не занимают. */
internal fun OfdSummary.rows(texts: KkmSetupTexts): List<Pair<String, String>> = listOfNotNull(
    answer?.let { texts.ofdAnswer to it },
    organization?.let { texts.ofdOrganization to it },
    address?.let { texts.ofdAddress to it },
    bin?.let { texts.ofdBin to it },
    kgdNumber?.let { texts.ofdKgdNumber to it },
    factoryNumber?.let { texts.ofdFactoryNumber to it },
    systemId?.let { texts.ofdSystemId to it },
    protocol?.let { texts.ofdProtocol to it }
)
