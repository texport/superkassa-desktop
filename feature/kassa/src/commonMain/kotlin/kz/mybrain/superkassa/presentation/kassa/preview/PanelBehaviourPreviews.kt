package kz.mybrain.superkassa.presentation.kassa.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.kassa.sale.PanelBehaviourGroup
import kz.mybrain.superkassa.presentation.kassa.sale.SalePanel

/** Раздел «Экран продажи» в настройках: деньги и реквизиты свёрнуты, остальное открыто. */
@ElementPreviews
@Composable
private fun PanelBehaviourPreview() = PreviewTheme {
    PanelBehaviourGroup(expanded = { it !in COLLAPSED }, onToggle = {})
}

private val COLLAPSED = setOf(SalePanel.CustomerData, SalePanel.Money)
