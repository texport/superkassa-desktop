package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.loadDay
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.LocalDate

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
fun ReturnsScreen(session: Session) {
    val scope = rememberCoroutineScope()
    val texts = journalTexts(session.language)
    val journal = texts.returns
    var kind by remember { mutableStateOf(ReturnKind.Sell) }
    var basisId by remember { mutableStateOf<String?>(null) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var number by remember { mutableStateOf("") }
    val documents = remember { mutableStateListOf<Document>() }
    var loading by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }

    // День перечитывается и после пробитого чека: возврат по только что
    // выданному чеку — обычное дело, а список, набранный при открытии
    // экрана, о нём не знает.
    LaunchedEffect(day, session.selected?.kkmId, session.documents.size) {
        documents.clear()
        loading = true
        more = loadDay(session, journal.basis, day, documents)
        loading = false
    }

    val candidates = kind.basisIn(documents).filter { it.matches(number) }
    val chosen = candidates.firstOrNull { it.id == basisId }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ReturnHeader(journal, kind) {
            kind = it
            basisId = null
        }
        BasisSearch(texts.history, day, number, loading, onNumber = { number = it }) {
            day = it
            basisId = null
        }
        val state = when {
            // Закрытая смена — состояние, а не отказ: об этом сказано словами
            // и подсказкой, а не пустым списком, из которого ничего не понять.
            !session.shiftOpen -> ScreenState.Empty(AppIcons.noBasis, journal.shiftClosed, journal.shiftClosedHint)
            loading && candidates.isEmpty() -> ScreenState.Working
            candidates.isEmpty() -> ScreenState.Empty(AppIcons.noBasis, kind.emptyText(journal), journal.noBasisHint)
            else -> ScreenState.Ready
        }
        ScreenSlot(state, Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.normal)
            ) {
                BasisList(
                    candidates = candidates,
                    chosen = chosen,
                    journal = journal,
                    history = texts.history,
                    more = more,
                    loading = loading,
                    modifier = Modifier.weight(BASIS_COLUMN),
                    onMore = {
                        loading = true
                        scope.launch {
                            more = loadDay(session, journal.basis, day, documents)
                            loading = false
                        }
                    }
                ) { basisId = it.id }
                RefundPanel(session, kind, chosen, Modifier.weight(REFUND_COLUMN)) { basisId = null }
            }
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texts.returns.title, style = MaterialTheme.typography.headlineSmall)
        // В сегменте стоит только направление: «Возврат» уже написано
        // заголовком слева.
        ChoiceSegments(
            options = ReturnKind.entries,
            selected = kind,
            label = { it.shortTitle(texts.sale) },
            onSelect = onKind
        )
        // Правило возврата — под значком: кассир читает его один раз,
        // а место на экране оно занимало бы в каждой смене.
        InfoTip(journal.basisHint)
    }
}

/** Доли ширины: список чеков шире панели, в нём читают, а не вводят. */
private const val BASIS_COLUMN = 1.3f
private const val REFUND_COLUMN = 1f
