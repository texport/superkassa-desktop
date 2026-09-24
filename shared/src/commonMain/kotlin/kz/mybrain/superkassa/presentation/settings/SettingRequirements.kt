package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.settings.model.KkmDemand
import kz.mybrain.superkassa.domain.settings.model.KkmNeed
import kz.mybrain.superkassa.strings.api.kassa.KkmSetupTexts

/**
 * Чего кассе не хватает, чтобы принять настройку.
 *
 * Стоит над кнопкой, которую сам же и гасит: касса откажет ровно по этим
 * требованиям, и кассир обязан видеть невыполненное до нажатия, а не
 * читать отказ после него.
 *
 * Требования переносятся, а не жмутся в строку: перенос оставляет на виду
 * все, в том числе невыполненное.
 *
 * Знак впереди подписи — не украшение: подпись у выполненного и
 * невыполненного требования одна и та же, и без знака их различал бы
 * только цвет плашки.
 */
@Composable
internal fun SettingRequirements(needs: List<KkmNeed>, texts: KkmSetupTexts) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        needs.forEach { need ->
            Chip(
                text = requirementLine(need, texts),
                color = if (need.met) StatusColors.delivered else StatusColors.refused
            )
        }
    }
}

/**
 * Надпись плашки: знак и требование.
 *
 * Собрана отдельно от рисования, чтобы проверять её без экрана: знак
 * и есть то, чем выполненное требование отличается от невыполненного,
 * когда цвет плашки совпал с основным тоном кассы.
 */
internal fun requirementLine(need: KkmNeed, texts: KkmSetupTexts): String {
    val mark = if (need.met) Glyphs.MET else Glyphs.UNMET
    return "$mark${Glyphs.NBSP}${need.demand.title(texts)}"
}

/** Требование кассы словами кассира. */
internal fun KkmDemand.title(texts: KkmSetupTexts): String = when (this) {
    KkmDemand.Programming -> texts.needProgramming
    KkmDemand.ShiftClosed -> texts.needShiftClosed
    KkmDemand.QueueEmpty -> texts.needQueueEmpty
    KkmDemand.Online -> texts.needOnline
}
