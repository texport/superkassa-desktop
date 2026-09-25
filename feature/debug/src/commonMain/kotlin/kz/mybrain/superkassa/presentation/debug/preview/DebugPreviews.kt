package kz.mybrain.superkassa.presentation.debug.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.debug.log.DebugCard
import kz.mybrain.superkassa.presentation.debug.log.LogActions
import kz.mybrain.superkassa.presentation.debug.log.LogUiState

/** Раздел «Отладка»: режим выключен, журнал обычного уровня. */
@ElementPreviews
@Composable
private fun DebugPreview() = PreviewTheme { DebugCard(LogUiState(), object : LogActions {}) }
