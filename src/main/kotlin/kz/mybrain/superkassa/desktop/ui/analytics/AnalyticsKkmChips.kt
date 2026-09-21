package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.desktop.ui.cabinet.statusTitle
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Состояние кассы плашками — одно и то же в карточке и в списке места.
 *
 * В списке касс места состояние сперва не показывалось вовсе, и красный
 * ярлычок на карте оставался необъяснённым: владелец видел, что в этом
 * доме что-то не так, открывал список и читал три одинаковые строки.
 * Плашки собраны здесь, а не в каждом из двух показов: одна касса
 * не должна выглядеть по-разному в двух местах одного экрана.
 */
@Composable
internal fun KkmChips(kkm: AnalyticsKkm, texts: AnalyticsTexts, cabinet: CabinetTexts) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        CabinetStatusChip(kkm.status, cabinet)
        if (kkm.blocked) Chip(text = texts.blocked, color = StatusColors.refused)
        kkm.shiftStatus?.takeIf { it.isNotBlank() }?.let { shift ->
            // Ожиданием красится только открытая смена: закрытая — обычное
            // состояние кассы, и жёлтым владелец читал её как незаконченное
            // дело, которое надо доделать.
            val open = KkmMark.ShiftOpen.holds(kkm)
            Chip(
                text = shiftWords(shift, kkm.shiftNumber, cabinet),
                color = if (open) StatusColors.pending else MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Смена: её состояние и номер одной плашкой. */
private fun shiftWords(status: String, number: Long?, cabinet: CabinetTexts): String =
    listOfNotNull(statusTitle(status, cabinet), number?.let { "№ $it" }).joinToString(" · ")
