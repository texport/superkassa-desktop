package kz.mybrain.superkassa.presentation.settings.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.presentation.settings.ofd.DiagnosticsCard
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSyncCard
import kz.mybrain.superkassa.presentation.settings.ofd.OfdTokenCard

/*
 * Группы раздела «Связь с БФД» по одной: диагностика с ответом БФД,
 * сверка и замена токена.
 */

@ElementPreviews
@Composable
private fun DiagnosticsPreview() = Group { DiagnosticsCard(it.ofd, it.ofdActions) }

@ElementPreviews
@Composable
private fun OfdSyncPreview() = Group { OfdSyncCard(it.ofd, it.ofdActions) }

@ElementPreviews
@Composable
private fun OfdTokenPreview() = Group { OfdTokenCard(it.ofd, it.ofdActions) }
