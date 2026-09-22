package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * бумаги, а не как увеличение. Подобранный масштаб держится до закрытия
 * кассы: сбрасывать его на каждый чек значит заставлять подбирать заново.
 *
 * Окно открывается по нажатию, а не по готовой картинке: узел рисует
 * форму секунду-другую, и всё это время в окне стоит общее ожидание —
 * иначе нажатие не отзывалось ничем.
 *
 * Отказ узла окна не закрывает: причина стоит в нём вместе с повтором.
 * Прежде окно исчезало целиком, и владелец оставался с одной строкой
 * внизу экрана, которую мог уже закрыть.
 *
 * @param image печатная форма в PNG; `null` — ещё не нарисована.
 * @param drawing узел сейчас рисует: окно открыто, картинки ещё нет.
 * @param trouble узел форму не нарисовал; `null` — беды нет.
 * @param onPrint отправка на принтер; `null` — печатать нечем.
 * @param onSave сохранение в файл; `null` — сохранять нечем.
 */
@Composable
fun ReceiptPreview(
    image: ByteArray?,
    drawing: Boolean = false,
    trouble: ScreenState.Trouble? = null,
    onPrint: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Масштаб ленты живёт выше окна и переживает его закрытие: кассир
    // подбирает ширину под свой экран один раз, а не заново у каждого чека.
    // Внутри окна он заводился вместе с ним и умирал вместе с ним же.
    var tapeWidth by remember { mutableStateOf(Tape.defaultWidth) }
    if (image == null && !drawing && trouble == null) return
    val texts = LocalStrings.current.preview
    CloseOnEscape(onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(Tape.DIALOG_FRACTION),
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            val bitmap = remember(image) {
                image?.let { runCatching { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull() }
            }
            val state = when {
                trouble != null -> trouble
                image == null -> ScreenState.Working
                bitmap == null -> ScreenState.Empty(AppIcons.warning, texts.missing)
                else -> ScreenState.Ready
            }
            Column(modifier = Modifier.fillMaxSize()) {
                PreviewBar(
                    tapeWidth = tapeWidth,
                    onWidth = { tapeWidth = it },
                    onPrint = onPrint.takeIf { bitmap != null },
                    onSave = onSave.takeIf { bitmap != null },
                    onDismiss = onDismiss
                )
                ScreenSlot(state, Modifier.fillMaxSize(), centered = true) {
                    bitmap?.let { tape ->
                        TapeView(bitmap = BitmapPainter(tape), width = tapeWidth, title = texts.title) {
                            tapeWidth = (tapeWidth + it).coerceIn(Tape.minWidth, Tape.maxWidth)
                        }
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
 * листают чаще, чем меняют масштаб. Прокруткой занимается сама область,
 * а не разбор события: своя прокрутка поверх родной боролась с ней
 * за ленту и проигрывала — щелчок колеса двигал ленту ровно настолько,
 * насколько её двигает родная, а написанный рядом шаг не значил ничего.
 *
 * Ctrl-щелчок разбирается раньше области и здесь же поглощается: без
 * этого одно движение колеса и меняло масштаб, и уезжало по ленте.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TapeView(bitmap: BitmapPainter, width: Dp, title: String, onZoom: (Dp) -> Unit) {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
                if (!event.keyboardModifiers.isCtrlPressed) return@onPointerEvent
                val change = event.changes.first()
                onZoom(Tape.widthStep * -change.scrollDelta.y)
                change.consume()
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
