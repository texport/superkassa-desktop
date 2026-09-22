package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.strings.KkmSetupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Чего узлу не хватает, чтобы принять настройку.
 *
 * Стоит над кнопкой, которую сам же и гасит: узел откажет ровно по этим
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        needs.forEach { need ->
            Chip(
                text = "${mark(need.met)}${Glyphs.NBSP}${need.demand.title(texts)}",
                color = if (need.met) StatusColors.delivered else StatusColors.refused
            )
        }
    }
}

private fun mark(met: Boolean): String = if (met) Glyphs.MET else Glyphs.UNMET
