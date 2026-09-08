package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.Tape
import org.jetbrains.skia.Image as SkiaImage

/**
 * Печатная форма документа.
 *
 * Образ рисует узел — тот же, который уходит на печать: свой рисунок дал бы
 * два разных чека по одному документу.
 *
 * Лента лежит на поверхности окна без подложки по бокам: цветные поля
 * вокруг чека кассир принимал за часть документа. Масштаб меняется колесом
 * мыши с Ctrl и значками — словами «уже» и «шире» это читалось как ширина
 * бумаги, а не как увеличение.
 *
 * @param image печатная форма в PNG; `null` — окно не показывается.
 * @param onPrint отправка на принтер; `null` — печатать нечем.
 * @param onSave сохранение в файл; `null` — сохранять нечем.
 */
@Composable
fun ReceiptPreview(
    image: ByteArray?,
    onPrint: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (image == null) return
    val texts = LocalStrings.current.preview
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(Tape.DIALOG_FRACTION),
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            var tapeWidth by remember { mutableStateOf(Tape.defaultWidth) }
            val bitmap = remember(image) {
                runCatching { SkiaImage.makeFromEncoded(image).toComposeImageBitmap() }.getOrNull()
            }
            Column(modifier = Modifier.fillMaxSize()) {
                PreviewBar(
                    tapeWidth = tapeWidth,
                    onWidth = { tapeWidth = it },
                    onPrint = onPrint,
                    onSave = onSave,
                    onDismiss = onDismiss
                )
                if (bitmap == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(texts.missing, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    TapeView(bitmap = BitmapPainter(bitmap), width = tapeWidth, title = texts.title) {
                        tapeWidth = (tapeWidth + it).coerceIn(Tape.minWidth, Tape.maxWidth)
                    }
                }
            }
        }
    }
}

/**
 * Сама лента: прокрутка по вертикали, масштаб колесом с Ctrl.
 *
 * Колесо без Ctrl прокручивает, как в любом просмотрщике: длинный Z-отчёт
 * листают чаще, чем меняют масштаб.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TapeView(bitmap: BitmapPainter, width: Dp, title: String, onZoom: (Dp) -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize().onPointerEvent(PointerEventType.Scroll) { event ->
            val change = event.changes.first()
            val step = change.scrollDelta.y
            if (event.keyboardModifiers.isCtrlPressed) {
                onZoom(Tape.widthStep * -step)
            } else {
                scope.launch { scroll.scrollBy(step * Tape.SCROLL_STEP) }
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = bitmap,
                contentDescription = title,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.padding(vertical = Tape.margin).width(width)
            )
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(Spacing.hairline)
        )
    }
}

/** Строка действий над формой: печать, сохранение, масштаб и закрытие. */
@Composable
private fun PreviewBar(
    tapeWidth: Dp,
    onWidth: (Dp) -> Unit,
    onPrint: (() -> Unit)?,
    onSave: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val texts = LocalStrings.current.preview
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.roomy, vertical = Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(texts.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        onPrint?.let { print ->
            IconButton(onClick = print) { Icon(AppIcons.print, contentDescription = texts.print) }
        }
        onSave?.let { save ->
            IconButton(onClick = save) { Icon(AppIcons.save, contentDescription = texts.save) }
        }
        IconButton(
            enabled = tapeWidth > Tape.minWidth,
            onClick = { onWidth((tapeWidth - Tape.widthStep).coerceAtLeast(Tape.minWidth)) }
        ) { Icon(AppIcons.zoomOut, contentDescription = texts.zoomOut) }
        IconButton(
            enabled = tapeWidth < Tape.maxWidth,
            onClick = { onWidth((tapeWidth + Tape.widthStep).coerceAtMost(Tape.maxWidth)) }
        ) { Icon(AppIcons.zoomIn, contentDescription = texts.zoomIn) }
        IconButton(onClick = onDismiss) { Icon(AppIcons.close, contentDescription = texts.close) }
    }
}
