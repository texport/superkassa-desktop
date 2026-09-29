package kz.mybrain.superkassa.presentation.shift.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.TwoPane
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.KassaLayout
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.document.model.printable
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.shift.dashboard.component.AutonomousCard
import kz.mybrain.superkassa.presentation.shift.dashboard.component.DashboardPage
import kz.mybrain.superkassa.presentation.shift.dashboard.component.RefusedDocuments
import kz.mybrain.superkassa.presentation.shift.dashboard.component.RefusedSummary
import kz.mybrain.superkassa.presentation.shift.dashboard.component.ShiftActions
import kz.mybrain.superkassa.presentation.shift.dashboard.component.ShiftDocuments
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Главный экран: состояние выбранной кассы и документы текущей смены.
 *
 * Показывается именно смена, а не весь журнал: кассиру в течение дня нужна
 * своя смена, а история — отдельный раздел.
 */
@Composable
fun DashboardScreen(model: DashboardViewModel, print: PrintActions) {
    val state by model.state.collectAsScreenState()
    // Продажа, возврат и деньги меняют смену: вернувшись, кассир видит их.
    LaunchedEffect(model) { model.refresh() }
    DashboardContent(state, model.actions(print))
}

/** Главный экран по готовому состоянию: снимки вида рисуют его без модели. */
@Composable
fun DashboardContent(state: DashboardUiState, actions: DashboardActions = object : DashboardActions {}) {
    val texts = LocalStrings.current
    if (state.kkm == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
        ) {
            Text(texts.topBar.noKkm, style = MaterialTheme.typography.titleMedium)
            Text(texts.login.pickHint, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    DashboardPage(
        top = {
            StatTiles(state)
            AutonomousCard(state, actions)
            ShiftActions(state, actions)
        },
        lists = { size -> ShiftBody(state, actions, size) }
    )
}

/**
 * Числа смены плитками.
 *
 * Плитки переносятся, а не сжимаются: остаток ящика в миллиарды в трети
 * узкого окна переносился посреди числа. Плитки одного ряда одной высоты,
 * хотя остаток набран крупнее соседей.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatTiles(state: DashboardUiState) {
    val texts = LocalStrings.current
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.cardGap) {
        val tile = Modifier.weight(1f).widthIn(min = KassaLayout.statTile).fillMaxRowHeight()
        val drawer = Modifier.weight(KassaLayout.DRAWER_TILE_WEIGHT).widthIn(min = KassaLayout.drawerTile)
        // Состояние кассы и смены стоит в шапке и повторено здесь не будет:
        // одно и то же слово в двух местах экрана расходится на первой же
        // правке. Плиткам остаются числа смены.
        // Номер смены берётся из ответа кассы о смене, а не из записи
        // кассы: у кассы номер последней смены отстаёт, а плитка
        // называет ту смену, которую касса держит сейчас.
        StatCard(texts.dashboard.shift, tile) { StatText(shiftValue(state)) }
        // Плитка, карточка ящика и подтверждение Z-отчёта называют
        // остаток одним именем: три названия одного числа кассир читал
        // как три разных счётчика. Набран он тем же начертанием, что
        // в «Деньгах», — главным числом, одной строкой.
        StatCard(textsOf(LocalLanguage.current).kassa.money.drawer.inDrawer, drawer.fillMaxRowHeight()) {
            MoneyText(Money.formatTiyn(state.cashInDrawer), Modifier.fillMaxWidth(), MoneyStyle.hero)
        }
        // Число документов — только там, где касса их назвала. Непрочитанный
        // список показывался нулём, и «за смену не пробито ничего»
        // стояло над сменой, документы которой касса отдать отказалась.
        StatCard(texts.dashboard.documentsInShift, tile) {
            StatText(if (state.documentsRead) state.documents.size.toString() else Glyphs.DASH)
        }
    }
}

/**
 * Отклонённые БФД и документы смены.
 *
 * Пока отказов нет, списку смены отдано всё место. Есть — они стоят
 * рядом со списком, а на тесном месте над ним, но список берёт свою долю
 * высоты первым: пачка отказов выдавливала документы смены за нижний
 * край окна. Ниже [KassaLayout.refusedListRoom] от отказов остаётся
 * строка с их числом: перечень там резался посреди строки.
 */
@Composable
private fun ShiftBody(state: DashboardUiState, actions: DashboardActions, modifier: Modifier) {
    if (state.documents.all { it.printable }) {
        ShiftDocuments(state, actions, modifier)
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxHeight < KassaLayout.refusedListRoom) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                RefusedSummary(state)
                ShiftDocuments(state, actions, Modifier.weight(1f))
            }
        } else {
            TwoPane(
                split = KassaLayout.refusedAndDocuments,
                modifier = Modifier.fillMaxSize(),
                first = { RefusedDocuments(state) },
                second = { ShiftDocuments(state, actions, Modifier.fillMaxSize()) }
            )
        }
    }
}

/** Состояние смены словами кассы; неизвестное состояние так и называется. */
@Composable
private fun shiftValue(state: DashboardUiState): String {
    val texts = LocalStrings.current
    return when (state.shift) {
        ShiftState.Open -> texts.dashboard.shiftOpenNo.fill(state.shiftNumber ?: Glyphs.DASH)
        ShiftState.Closed -> texts.dashboard.shiftClosed
        ShiftState.Unknown -> texts.dashboard.shiftUnknown
    }
}

@Composable
private fun StatCard(caption: String, modifier: Modifier, value: @Composable () -> Unit) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            value()
        }
    }
}

@Composable
private fun StatText(value: String) {
    Text(value, style = MaterialTheme.typography.headlineSmall)
}
