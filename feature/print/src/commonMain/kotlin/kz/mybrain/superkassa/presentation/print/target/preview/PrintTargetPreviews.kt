package kz.mybrain.superkassa.presentation.print.target.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.print.target.PrintTargetActions
import kz.mybrain.superkassa.presentation.print.target.PrintTargetCard
import kz.mybrain.superkassa.presentation.print.target.PrintTargetUiState

/** Принтер кассы в разделе печати: машина видит чековый принтер. */
@ElementPreviews
@Composable
private fun PrintTargetPreview() = PreviewTheme {
    PrintTargetCard(PrintTargetUiState(kkmId = KKM, printers = listOf("Чековый у кассы"), printersRead = true), NONE)
}

/** Принтеров на машине нет: группа говорит об этом словами. */
@ElementPreviews
@Composable
private fun NoPrinterPreview() = PreviewTheme {
    PrintTargetCard(PrintTargetUiState(kkmId = KKM, printersRead = true), NONE)
}

private const val KKM = "kkm-1"
private val NONE = object : PrintTargetActions {}
