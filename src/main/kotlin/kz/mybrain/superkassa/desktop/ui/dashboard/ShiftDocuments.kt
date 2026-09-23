package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.hasOwnAmount
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.history.DocumentDeliveryChip
import kz.mybrain.superkassa.desktop.ui.strings.DashboardStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Документы текущей смены.
 *
 * Список, а не карточка на строку: за смену их сотни, и рамка вокруг
 * каждой превращает журнал в лоскутное одеяло. Плотность та же, что
 * в разделе истории, — кассир читает обе таблицы одинаково.
 */
@Composable
fun ShiftDocuments(session: Session, modifier: Modifier = Modifier) {
    val texts = LocalStrings.current

    val scope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        Text(texts.dashboard.shiftDocuments, style = MaterialTheme.typography.titleMedium)
        val state = documentsState(session, texts.dashboard) {
            scope.launch { session.refreshSelected() }
        }
        ScreenSlot(state, dense = true) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ScrollableList {
                    items(session.documents) { document ->
                        DocumentRow(
                            document = document,
                            documentTitle = session.titleOf(Dictionary.DocumentTypes, document.docType),
                            onPreview = { session.printDesk.preview(document) },
                            onPrint = { session.printDesk.print(document) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Что стоит на месте списка документов.
 *
 * Пустой список и непрочитанный список — разные вещи: у кассы, снятой
 * с учёта, узел отвечает на документы открытой смены KKM_BLOCKED, и на
 * этом месте стояло «Документов пока нет» с обещанием, что первый чек
 * вот-вот появится. Кассир читал это как пустую смену.
 */
private fun documentsState(session: Session, texts: DashboardStrings, onRetry: () -> Unit): ScreenState = when {
    session.documents.isNotEmpty() -> ScreenState.Ready
    // Список приходит вместе с состоянием смены: до ответа узла пустота
    // читалась как «за смену не пробито ничего».
    session.busy -> ScreenState.Working
    session.shiftState == ShiftState.Open && !session.documentsRead ->
        ScreenState.Trouble(texts.documentsUnread, texts.documentsUnreadHint, onRetry)

    else -> ScreenState.Empty(
        icon = AppIcons.history,
        // Своё название, а не повтор заголовка над списком: два
        // одинаковых «Документы смены» подряд ничего не добавляли.
        title = texts.shiftEmpty,
        hint = emptyHint(session, texts)
    )
}

/**
 * Чем объясняется пустой список.
 *
 * Состояний смены три, и подсказка у каждого своя. Прежде их было две:
 * состояние, которого узел не назвал, объявлялось закрытой сменой —
 * при плитке «Смена: Неизвестна» прямо над списком, — а открытой смене
 * подсказкой доставалось одно слово «Чек».
 */
private fun emptyHint(session: Session, texts: DashboardStrings): String = when (session.shiftState) {
    ShiftState.Open -> texts.shiftEmptyHint
    ShiftState.Closed -> texts.openShiftHint
    ShiftState.Unknown -> texts.shiftUnknownHint
}

@Composable
private fun DocumentRow(
    document: Document,
    documentTitle: String,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    val texts = LocalStrings.current
    RecordRow(
        title = documentTitle,
        // Номер подписан: голая «1» под словом «Продажа» читалась как
        // количество, а не как номер документа.
        subtitle = document.number?.let { "${texts.dashboard.documentNo} $it" } ?: Glyphs.DASH,
        amount = documentAmount(document),
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocumentDeliveryChip(document)
                // Код отказа вместо кнопки повтора: документ, который ОФД
                // отверг, повторной отправкой не исправить — операцию нужно
                // провести заново. Кассиру полезен не повтор, а причина.
                if (document.refusalCode != null) {
                    Text(
                        text = "${texts.common.refusalCode} ${document.refusalCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // Два действия, а не одно: «просмотр» показывает форму
                // на экране, «печать» отправляет её на принтер рабочего
                // места. Раньше кнопка называлась печатью, а печатала
                // только в окно.
                // У отклонённого ОФД документа печатной формы нет вовсе:
                // она выглядит как настоящий чек, а фискальным он не стал.
                IconButton(enabled = document.printable, onClick = onPreview) {
                    Icon(AppIcons.preview, contentDescription = texts.preview.title)
                }
                IconButton(enabled = document.printable, onClick = onPrint) {
                    Icon(AppIcons.print, contentDescription = texts.preview.print)
                }
            }
        }
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * Сумма документа в строке смены.
 *
 * У отчёта и открытия смены своей суммы нет: узел держит у них ноль,
 * и «0,00 ₸» рядом с X-отчётом кассир читал как «не продано ничего».
 * Журнал за срок на том же месте ставит прочерк, а обе таблицы кассир
 * читает одинаково.
 */
internal fun documentAmount(document: Document): String =
    if (document.hasOwnAmount) Money.formatTiyn(document.totalAmount) else Glyphs.DASH
