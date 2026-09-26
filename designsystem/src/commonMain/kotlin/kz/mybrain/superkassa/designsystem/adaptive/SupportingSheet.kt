package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Вспомогательная панель нижним листом Material 3 (стандартный
 * `BottomSheetScaffold`, не модальный): главная видна над ним и работает.
 *
 * Свёрнутый лист показывает ручку и сводку — высота свёрнутого листа
 * берётся из самой сводки, а не из подобранного числа; развёрнутый —
 * ещё и всё остальное. Главная панель отступает снизу на свёрнутый лист,
 * и её низ под ним не прячется. Лист тянут за ручку — ручка Material 3
 * сама разворачивает и сворачивает его и нажатием; второй кнопки рядом
 * нет, чтобы не спорить со стрелками разделов внутри.
 *
 * Лист прикреплён к нижнему краю окна и поле окна снизу перекрывает
 * ([LocalFrameBottom]): висящий над краем лист не читался бы листом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupportingSheet(
    expanded: Boolean,
    onToggle: () -> Unit,
    main: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    summary: @Composable () -> Unit
) {
    val sheet = rememberStandardBottomSheetState(
        initialValue = if (expanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    SheetFollows(sheet, expanded, onToggle)
    var peek by remember { mutableStateOf(Spacing.flush) }
    val density = LocalDensity.current
    BottomSheetScaffold(
        modifier = Modifier.bleedToWindowEdge(LocalFrameBottom.current),
        scaffoldState = rememberBottomSheetScaffoldState(sheet),
        sheetPeekHeight = peek,
        sheetDragHandle = null,
        containerColor = Color.Transparent,
        sheetContent = {
            SheetBody(Modifier.onSizeChanged { peek = with(density) { it.height.toDp() } }, summary, supporting)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = peek.bottomGap())) { main() }
    }
}

/**
 * Лист и модель знают одно и то же: кнопка сводки двигает лист, а лист,
 * дотянутый жестом, сообщает модели, развёрнут ли он.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetFollows(sheet: SheetState, expanded: Boolean, onToggle: () -> Unit) {
    LaunchedEffect(expanded) { if (expanded) sheet.expand() else sheet.partialExpand() }
    LaunchedEffect(sheet.currentValue) { if ((sheet.currentValue == SheetValue.Expanded) != expanded) onToggle() }
}

/**
 * Развёрнутый лист во всю высоту: сверху ручка и сводка — не выше доли
 * [Panes.STACKED_SECOND_SHARE] листа, лишнее сводка прокручивает сама, —
 * под ними всё остальное. Иначе развёрнутая сводка с пятью видами оплаты
 * выталкивала остальное за край.
 *
 * @param peek размер ручки со сводкой — по нему лист и сворачивается.
 */
@Composable
private fun SheetBody(peek: Modifier, summary: @Composable () -> Unit, supporting: @Composable () -> Unit) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            SheetPeek(peek, summary)
            Box(modifier = Modifier.padding(Spacing.cardPadding)) { supporting() }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val cap = (constraints.maxHeight * Panes.STACKED_SECOND_SHARE).toInt()
        val head = measurables[0].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = cap))
        val rest = (constraints.maxHeight - head.height).coerceAtLeast(0)
        val body = measurables[1].measure(Constraints.fixed(width, rest))
        layout(width, constraints.maxHeight) {
            head.place(0, 0)
            body.place(0, head.height)
        }
    }
}

/** Свёрнутый лист: ручка и сводка — по их росту лист и сворачивается. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetPeek(modifier: Modifier, summary: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
        Box(modifier = Modifier.padding(horizontal = Spacing.cardPadding)) { summary() }
    }
}

/** Главная панель отступает от свёрнутого листа ещё на шаг между карточками. */
private fun Dp.bottomGap(): Dp = this + Spacing.cardGap
