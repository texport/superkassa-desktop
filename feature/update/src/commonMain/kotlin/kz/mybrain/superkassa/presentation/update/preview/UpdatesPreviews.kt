package kz.mybrain.superkassa.presentation.update.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.update.check.UpdatesActions
import kz.mybrain.superkassa.presentation.update.check.UpdatesCard
import kz.mybrain.superkassa.presentation.update.check.UpdatesUiState

/** Раздел «Обновления»: проверка по расписанию включена, выпусков ещё не проверяли. */
@ElementPreviews
@Composable
private fun UpdatesPreview() = PreviewTheme { UpdatesCard(UpdatesUiState(), object : UpdatesActions {}) }
