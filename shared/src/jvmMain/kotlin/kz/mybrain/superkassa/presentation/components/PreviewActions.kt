package kz.mybrain.superkassa.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.Tape

/**
 * Что можно сделать с открытой формой.
 *
 * @param onPrint отправка на принтер; `null` — печатать нечем.
 * @param onSave сохранение в файл; `null` — сохранять нечем.
 */
internal class PreviewActions(
    val tapeWidth: Dp,
    val onWidth: (Dp) -> Unit,
    val onPrint: (() -> Unit)?,
    val onSave: (() -> Unit)?,
    val onDismiss: () -> Unit
)

/**
 * Шапка окна печатной формы на узком окне — полноэкранный диалог Material 3.
 *
 * Закрытие стоит первым слева, за ним название; справа — масштаб
 * значками и действия словами, главное последним и залитым.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenPreviewBar(actions: PreviewActions) {
    val texts = LocalStrings.current.preview
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        navigationIcon = {
            IconButton(onClick = actions.onDismiss, modifier = Modifier.padding(start = Spacing.tight)) {
                Icon(AppIcons.close, contentDescription = texts.close)
            }
        },
        title = { Text(texts.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = Spacing.normal)
            ) {
                ZoomButtons(actions)
                NamedActions(actions)
            }
        }
    )
}

/**
 * Шапка окна печатной формы на широком окне — окно с полями вокруг.
 *
 * Здесь форма — не полноэкранный диалог, а окно поверх раздела, и его
 * закрывают, как окно, значком в правом углу. Действия названы словами,
 * «Печать» — залитой кнопкой: прежде все пять стояли одинаковыми
 * значками, и главное узнавали только по картинке принтера.
 */
@Composable
internal fun WindowedPreviewBar(actions: PreviewActions) {
    val texts = LocalStrings.current.preview
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.roomy, vertical = Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(
            text = texts.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        NamedActions(actions)
        ZoomButtons(actions)
        IconButton(onClick = actions.onDismiss) { Icon(AppIcons.close, contentDescription = texts.close) }
    }
}

/** Сохранение и печать — словами; печать главная и стоит залитой. */
@Composable
private fun NamedActions(actions: PreviewActions) {
    val texts = LocalStrings.current.preview
    actions.onSave?.let { save -> TextButton(onClick = save) { Text(texts.save) } }
    actions.onPrint?.let { print ->
        Button(onClick = print) {
            Icon(AppIcons.print, contentDescription = null)
            Text(texts.print, modifier = Modifier.padding(start = Spacing.tight))
        }
    }
}

/** Меньше и больше: масштаб ленты на экране, а не ширина бумаги. */
@Composable
private fun ZoomButtons(actions: PreviewActions) {
    val texts = LocalStrings.current.preview
    IconButton(
        enabled = actions.tapeWidth > Tape.minWidth,
        onClick = { actions.onWidth((actions.tapeWidth - Tape.widthStep).coerceAtLeast(Tape.minWidth)) }
    ) { Icon(AppIcons.zoomOut, contentDescription = texts.zoomOut) }
    IconButton(
        enabled = actions.tapeWidth < Tape.maxWidth,
        onClick = { actions.onWidth((actions.tapeWidth + Tape.widthStep).coerceAtMost(Tape.maxWidth)) }
    ) { Icon(AppIcons.zoomIn, contentDescription = texts.zoomIn) }
}
