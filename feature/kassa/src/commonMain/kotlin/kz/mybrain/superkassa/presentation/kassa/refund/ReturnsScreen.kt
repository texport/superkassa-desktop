package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.NarrowPanes
import kz.mybrain.superkassa.designsystem.adaptive.TwoPane
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.kassa.refund.component.BasisList
import kz.mybrain.superkassa.presentation.kassa.refund.component.BasisSearch
import kz.mybrain.superkassa.presentation.kassa.refund.component.RefundPanel
import kz.mybrain.superkassa.presentation.kassa.refund.component.basisState
import kz.mybrain.superkassa.presentation.words.kassa.shortTitle
import kz.mybrain.superkassa.strings.api.journal.ReturnJournalTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Возврат по чеку-основанию.
 *
 * Две колонки: слева чеки, годные в основание, справа — сумма возврата
 * и единственное главное действие экрана. Возврат по возврату протокол
 * не допускает, и такие чеки в список не попадают: кассир не должен
 * узнавать о запрете из отказа ОФД.
 *
 * Основание ищется по дню, а не берётся из открытой смены: покупатель
 * приходит с чеком позавчерашнего дня, и протокол этого не запрещает —
 * чек-основание описывается номером, датой и итогом, а смена в нём
 * не участвует. Сама же операция возврата требует открытой смены,
 * и это остаётся условием экрана.
 */
@Composable
fun ReturnsScreen(model: ReturnsViewModel) {
    val state by model.state.collectAsScreenState()
    val actions = remember(model) { model.actions() }
    // День перечитывается и при каждом входе: возврат по только что
    // выданному чеку — обычное дело, а прочитанный раньше день о нём не знает.
    LaunchedEffect(model) { model.visit() }
    ReturnsContent(state, actions)
}

/** Возврат по готовому состоянию: снимки вида рисуют его без модели. */
@Composable
fun ReturnsContent(state: ReturnsUiState, actions: ReturnsActions = ReturnsActions()) {
    val texts = textsOf(LocalLanguage.current).journal
    val journal = texts.returns
    val chosen = state.basis
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        ReturnHeader(journal, state.kind, actions.basis::kind)
        BasisSearch(texts.history, state.day, state.number, state.loading, actions.basis::number, actions.basis::day)
        ScreenSlot(basisState(state, journal, actions.basis::rereadDay), Modifier.weight(1f)) {
            // Список и панель возврата — рядом, пока обеим хватает места;
            // на узком окне выбранный чек открывает панель вместо списка.
            TwoPane(
                split = Panes.listDetail,
                modifier = Modifier.fillMaxWidth().weight(1f),
                narrow = NarrowPanes.Switched(showSecond = chosen != null),
                first = {
                    BasisList(state.candidates, chosen, journal, Modifier.fillMaxSize(), actions.basis::choose)
                },
                second = { RefundPanel(state, actions, Modifier.fillMaxSize()) }
            )
        }
    }
}

/**
 * Заголовок экрана и направление возврата.
 *
 * Направлений два и они исключают друг друга — это выбор одного из двух,
 * а не отбор, поэтому сегменты, а не плашки: возврат продажи выдаёт деньги
 * покупателю, возврат покупки принимает их обратно, и путать их нельзя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ReturnHeader(
    journal: ReturnJournalTexts,
    kind: ReturnKind,
    onKind: (ReturnKind) -> Unit
) {
    val texts = LocalStrings.current
    // Ряд переносится, а не сжимается: сегменты в узком окне обрезали
    // подпись до «Сатып а» без многоточия.
    WrapRow(spacing = Spacing.cardGap) {
        // В сегменте стоит только направление: «Возврат» уже написано
        // в шапке окна.
        ChoiceSegments(
            options = ReturnKind.entries,
            selected = kind,
            label = { it.shortTitle(texts.receipt) },
            onSelect = onKind
        )
        // Правило возврата — под значком: кассир читает его один раз,
        // а место на экране оно занимало бы в каждой смене.
        InfoTip(journal.basisHint)
    }
}
