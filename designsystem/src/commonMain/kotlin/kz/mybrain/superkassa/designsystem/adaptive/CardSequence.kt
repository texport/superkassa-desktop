package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.CardGrid

/**
 * Карточки, которые читаются по порядку, — одна под другой.
 *
 * Шаги мастера и разделы компании — последовательность: второй шаг
 * читается после первого. Столбцами [CardColumns] они читались «1, 4 / 2, 3»
 * и переставлялись, когда нажатие меняло высоту одного шага. Зазор — тот же,
 * что между карточками в столбцах.
 */
@Composable
fun CardSequence(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CardGrid.gap)) { content() }
}
