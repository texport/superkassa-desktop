package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.desktop.ui.cabinet.statusWords
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
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
        // Плашки нет вовсе, пока о смене ничего не известно: у кассы-черновика
        // кабинет отдаёт `UNKNOWN`, и это слово так и стояло на экране. Смены
        // у такой кассы не было ни одной, и сказать о ней нечего.
        shiftPlate(kkm.shiftStatus, kkm.shiftNumber, cabinet)?.let { shift ->
            // Ожиданием красится только открытая смена: закрытая — обычное
            // состояние кассы, и жёлтым владелец читал её как незаконченное
            // дело, которое надо доделать.
            val tone = if (KkmMark.ShiftOpen.holds(kkm)) StatusTone.Waiting else StatusTone.Idle
            Chip(text = shift, color = toneColor(tone))
        }
    }
}

/** Смена: её состояние и номер одной плашкой; `null` — состояние неизвестно. */
internal fun shiftPlate(status: String?, number: Long?, cabinet: CabinetTexts): String? {
    val words = status?.takeIf { it.isNotBlank() }?.let { statusWords(it, cabinet) } ?: return null
    return listOfNotNull(words, number?.let { "№ $it" }).joinToString(" · ")
}
