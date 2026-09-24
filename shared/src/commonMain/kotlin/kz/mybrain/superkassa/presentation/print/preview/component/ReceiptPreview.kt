package kz.mybrain.superkassa.presentation.print.preview.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.presentation.common.image.encodedImage
import kz.mybrain.superkassa.presentation.common.image.zoomByWheel
import kz.mybrain.superkassa.presentation.common.keyboard.CloseOnEscape
import kz.mybrain.superkassa.presentation.common.list.ColumnScrollbar
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.theme.size.Tape

/**
 * Печатная форма документа.
 *
 * Образ рисует касса — тот же, который уходит на печать: свой рисунок дал бы
 * два разных чека по одному документу.
 *
 * Лента лежит на поверхности окна без подложки по бокам: цветные поля
 * вокруг чека кассир принимал за часть документа. Масштаб меняется колесом
 * мыши с Ctrl и значками — словами «уже» и «шире» это читалось как ширина
 * бумаги, а не как увеличение. Подобранный масштаб держится до закрытия
 * кассы: сбрасывать его на каждый чек значит заставлять подбирать заново.
 *
 * Окно открывается по нажатию, а не по готовой картинке: касса рисует
 * форму секунду-другую, и всё это время в окне стоит общее ожидание —
 * иначе нажатие не отзывалось ничем.
 *
 * Отказ кассы окна не закрывает: причина стоит в нём вместе с повтором.
 * Прежде окно исчезало целиком, и владелец оставался с одной строкой
 * внизу экрана, которую мог уже закрыть.
 *
 * @param image печатная форма в PNG; `null` — ещё не нарисована.
 * @param drawing касса сейчас рисует: окно открыто, картинки ещё нет.
 * @param trouble касса форму не нарисовала; `null` — беды нет.
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
        // Форма занимает окно целиком, как полноэкранный диалог Material,
        // и на узком окне, и на широком: закрытие стоит слева, где его ищут
        // у полноэкранного окна. Окном с полями на широком мониторе закрытие
        // уезжало в правый угол, а раздел за полями отвлекал от ленты.
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            val bitmap = remember(image) {
                image?.let(::encodedImage)
            }
            val state = when {
                trouble != null -> trouble
                image == null -> ScreenState.Working
                bitmap == null -> ScreenState.Empty(AppIcons.warning, texts.missing)
                else -> ScreenState.Ready
            }
            Column(modifier = Modifier.fillMaxSize()) {
                val actions = PreviewActions(
                    tapeWidth = tapeWidth,
                    onWidth = { tapeWidth = it },
                    onPrint = onPrint.takeIf { bitmap != null },
                    onSave = onSave.takeIf { bitmap != null },
                    onDismiss = onDismiss
                )
                FullScreenPreviewBar(actions)
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
 * Сама лента: прокрутка по вертикали, масштаб колесом с Ctrl ([zoomByWheel]).
 *
 * Прокруткой занимается сама область, а не разбор события: своя прокрутка
 * поверх родной боролась с ней за ленту и проигрывала — щелчок колеса
 * двигал ленту ровно настолько, насколько её двигает родная, а написанный
 * рядом шаг не значил ничего.
 */
@Composable
private fun TapeView(bitmap: BitmapPainter, width: Dp, title: String, onZoom: (Dp) -> Unit) {
    val scroll = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize().zoomByWheel { clicks -> onZoom(Tape.widthStep * clicks) }) {
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
        ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(Spacing.inline))
    }
}
