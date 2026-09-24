package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.section.FactLines
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.settings.model.OfdSummary
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.strings.api.kassa.KkmSetupTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Диагностика кассы: связь с ОФД и сведения о кассе у него.
 *
 * Режим программирования отсюда убран: выход из него стоял здесь, а вход —
 * двумя разделами выше, и кассир, вошедший в режим, искал выход по всему
 * экрану. И то и другое живёт теперь под самой кассой, в [ProgrammingCard].
 *
 * Сведения о самой кассе — версии, режим, протокол, хранилище — показывает
 * карточка «Сведения о кассе»: они не о связи, а о том, как касса работает.
 */
@Composable
fun DiagnosticsCard(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val texts = LocalStrings.current
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    ofd.kkm ?: return
    SectionCard(
        title = texts.settings.diagnostics,
        info = money.diagnosticsHint,
        // Ответ ОФД — не состояние кассы, а итог только что нажатой
        // проверки: он и остаётся здесь, в строке заголовка.
        trailing = { ofd.linkAlive?.let { LinkChip(it) } }
    ) {
        Checks(ofd, actions)
        NextRequest(ofd.nextRequest)
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

/**
 * Проверки: связь с ОФД, его сведения о кассе и номер следующего запроса.
 * Ничего в кассе не меняют. Номер касса отдаёт только администратору —
 * кассиру кнопка не показывается, чтобы не вести его к отказу.
 */
@Composable
private fun Checks(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val texts = LocalStrings.current.settings
    val busy = ofd.busy
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(enabled = !busy, onClick = actions::checkLink) { Text(texts.checkOfdLink) }
        OutlinedButton(enabled = !busy, onClick = actions::askInfo) { Text(texts.ofdInfo) }
        if (ofd.admin) {
            OutlinedButton(enabled = !busy, onClick = actions::askNextRequest) {
                Text(textsOf(LocalLanguage.current).settings.facts.ofdAuth)
            }
        }
    }
}

/** Номер следующего запроса к БФД, если его спросили; токена рядом нет намеренно. */
@Composable
private fun NextRequest(number: Int?) {
    number ?: return
    val facts = textsOf(LocalLanguage.current).settings.facts
    FactLines(facts.ofdAuth, listOf(facts.nextRequest to number.toString()), facts.unread)
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
