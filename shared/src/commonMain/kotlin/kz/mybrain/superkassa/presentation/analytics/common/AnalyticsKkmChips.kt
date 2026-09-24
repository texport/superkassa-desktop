package kz.mybrain.superkassa.presentation.analytics.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.presentation.analytics.map.KkmMark
import kz.mybrain.superkassa.presentation.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.presentation.cabinet.statusWords
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.status.StatusTone
import kz.mybrain.superkassa.presentation.common.status.toneColor
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
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
    return listOfNotNull(words, number?.let { "${Glyphs.NUMBER} $it" }).joinToString(Glyphs.SEPARATOR)
}
