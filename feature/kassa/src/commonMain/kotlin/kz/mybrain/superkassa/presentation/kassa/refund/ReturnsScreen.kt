package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.adaptive.listDetailDirective
import kz.mybrain.superkassa.designsystem.adaptive.listDetailValue
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.navigation.step.ReturnBasisKey
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.navigation.DetailStep
import kz.mybrain.superkassa.presentation.common.navigation.detailStep
import kz.mybrain.superkassa.presentation.kassa.payment.DocumentDoneCard
import kz.mybrain.superkassa.presentation.kassa.payment.ReceiptOutput
import kz.mybrain.superkassa.presentation.kassa.refund.component.BasisList
import kz.mybrain.superkassa.presentation.kassa.refund.component.BasisSearch
import kz.mybrain.superkassa.presentation.kassa.refund.component.RefundPanel
import kz.mybrain.superkassa.presentation.kassa.refund.component.basisState
import kz.mybrain.superkassa.presentation.kassa.refund.component.returnGate
import kz.mybrain.superkassa.presentation.words.kassa.shortTitle
import kz.mybrain.superkassa.presentation.words.kassa.title
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
/**
 * @param stepped чек открыт поверх списка шагом истории окна — на узком окне.
 */
@Composable
fun ReturnsScreen(model: ReturnsViewModel, stepped: Boolean = false, output: ReceiptOutput = ReceiptOutput()) {
    val state by model.state.collectAsScreenState()
    val actions = remember(model) { model.actions() }
    // День перечитывается и при каждом входе: возврат по только что
    // выданному чеку — обычное дело, а прочитанный раньше день о нём не знает.
    LaunchedEffect(model) { model.visit() }
    ReturnsContent(state, actions, stepped, output)
}

/**
 * Возврат по готовому состоянию: снимки вида рисуют его без модели.
 *
 * Список чеков и панель возврата — «список и подробности» Material 3:
 * рядом, начиная с расширенного окна; на узком выбранный чек открывается
 * поверх списка шагом истории окна ([detailStep]), и поиск над списком
 * уступает место панели — назад к нему ведёт стрелка в шапке окна.
 *
 * @param stepped чек открыт поверх списка шагом истории окна.
 * @param output что сделать с чеком возврата: показать, распечатать, поделиться.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ReturnsContent(
    state: ReturnsUiState,
    actions: ReturnsActions = ReturnsActions(),
    stepped: Boolean = false,
    output: ReceiptOutput = ReceiptOutput()
) {
    val texts = textsOf(LocalLanguage.current).journal
    val journal = texts.returns
    val directive = listDetailDirective()
    val step = detailStep(stepped, state.basis != null, beside = directive.maxHorizontalPartitions > 1)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        if (!step.over) {
            ReturnHeader(journal, state.kind, actions.basis::kind)
        }
        // Блокировка и закрытая смена запрещают возврат целиком — о них
        // сказано вместо обеих панелей; остальное — в панели списка.
        ScreenSlot(returnGate(state, journal) ?: ScreenState.Ready, Modifier.weight(1f)) {
            ReturnPanes(state, actions, output, directive, step, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** Список чеков и панель возврата — панелями «списка и подробностей». */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ReturnPanes(
    state: ReturnsUiState,
    actions: ReturnsActions,
    output: ReceiptOutput,
    directive: PaneScaffoldDirective,
    step: DetailStep,
    modifier: Modifier
) {
    val journal = textsOf(LocalLanguage.current).journal.returns
    ListDetailPaneScaffold(
        directive = directive,
        value = listDetailValue(directive, step.over),
        listPane = { AnimatedPane { BasisPane(state, actions, output) { step.opened(ReturnBasisKey) } } },
        detailPane = { AnimatedPane { RefundPanel(state, actions, Modifier.fillMaxSize()) } },
        modifier = modifier
    )
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

/**
 * Панель списка: поиск чека-основания над самим списком.
 *
 * Поиск отбирает список, и стоит он в его панели, как по Material 3
 * (Canonical layouts → List-detail): поле номера над списком прежде тянулось
 * через всё окно и начиналось на полпальца левее панели подробностей.
 * Оформленный возврат стоит над поиском, как пробитый чек над корзиной
 * продажи: чек возврата показывают и печатают сразу.
 */
@Composable
private fun BasisPane(state: ReturnsUiState, actions: ReturnsActions, output: ReceiptOutput, onOpened: () -> Unit) {
    val texts = textsOf(LocalLanguage.current).journal
    val basis = actions.basis
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        state.issued?.let { issued ->
            val kind = issued.kind.title(LocalStrings.current.returns)
            DocumentDoneCard(kind, issued.total, null, issued.documentId, output, actions.refund::next)
        }
        BasisSearch(texts.history, state.day, state.number, state.loading, basis::number, basis::day)
        ScreenSlot(basisState(state, texts.returns, basis::rereadDay), Modifier.weight(1f)) {
            BasisList(state.candidates, state.basis, texts.returns, Modifier.fillMaxSize()) {
                basis.choose(it)
                onOpened()
            }
        }
    }
}
