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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Вспомогательная панель нижним листом Material 3 (стандартный
 * `BottomSheetScaffold`, не модальный): главная видна над ним и работает.
 *
 * Свёрнутый лист показывает ручку и сводку — высота свёрнутого листа
 * берётся из самой сводки, а не из подобранного числа; развёрнутый —
 * ещё и всё остальное. Главная панель отступает снизу на свёрнутый лист,
 * и её низ под ним не прячется. Лист тянут за ручку и жестом, сводка
 * сворачивает и разворачивает его кнопкой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupportingSheet(
    expanded: Boolean,
    onToggle: () -> Unit,
    main: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    summary: @Composable (toggle: @Composable () -> Unit) -> Unit
) {
    val sheet = rememberStandardBottomSheetState(
        initialValue = if (expanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val open = sheet.targetValue == SheetValue.Expanded
    SheetFollows(sheet, expanded, onToggle)
    var peek by remember { mutableStateOf(Spacing.flush) }
    val density = LocalDensity.current
    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(sheet),
        sheetPeekHeight = peek,
        sheetDragHandle = null,
        containerColor = Color.Transparent,
        sheetContent = {
            SheetPeek(Modifier.onSizeChanged { peek = with(density) { it.height.toDp() } }) {
                summary { SheetToggle(open, onToggle) }
            }
            Box(modifier = Modifier.padding(Spacing.cardPadding)) { supporting() }
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

/** Свёрнутый лист: ручка и сводка — по их росту лист и сворачивается. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetPeek(modifier: Modifier, summary: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
        Box(modifier = Modifier.padding(horizontal = Spacing.cardPadding)) { summary() }
    }
}

/** Кнопка листа: развернуть свёрнутый, свернуть развёрнутый. */
@Composable
private fun SheetToggle(open: Boolean, onToggle: () -> Unit) {
    PanelToggle(if (open) AppIcons.bottomSheetHide else AppIcons.bottomSheetShow, open, onToggle)
}

/** Главная панель отступает от свёрнутого листа ещё на шаг между карточками. */
private fun Dp.bottomGap(): Dp = this + Spacing.cardGap
