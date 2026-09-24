package kz.mybrain.superkassa.presentation.shift.dashboard.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.list.RecordRow
import kz.mybrain.superkassa.designsystem.list.ScrollableList
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.document.model.printable
import kz.mybrain.superkassa.domain.document.model.refusalCode
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.presentation.common.document.DocumentDeliveryChip
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardActions
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState
import kz.mybrain.superkassa.strings.api.common.DashboardStrings

/**
 * Документы текущей смены.
 *
 * Список, а не карточка на строку: за смену их сотни, и рамка вокруг
 * каждой превращает журнал в лоскутное одеяло. Плотность та же, что
 * в разделе истории, — кассир читает обе таблицы одинаково.
 */
@Composable
internal fun ShiftDocuments(state: DashboardUiState, actions: DashboardActions, modifier: Modifier = Modifier) {
    val texts = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        Text(texts.dashboard.shiftDocuments, style = MaterialTheme.typography.titleMedium)
        ScreenSlot(documentsState(state, texts.dashboard, actions::refresh), dense = true) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ScrollableList {
                    items(state.documents) { document ->
                        DocumentRow(
                            document = document,
                            documentTitle = documentTitle(state, document.docType),
                            onPreview = { actions.preview(document) },
                            onPrint = { actions.print(document) }
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
 * с учёта, касса отвечает на документы открытой смены KKM_BLOCKED, и на
 * этом месте стояло «Документов пока нет» с обещанием, что первый чек
 * вот-вот появится. Кассир читал это как пустую смену.
 */
private fun documentsState(state: DashboardUiState, texts: DashboardStrings, onRetry: () -> Unit): ScreenState = when {
    state.documents.isNotEmpty() -> ScreenState.Ready
    // Список приходит вместе с состоянием смены: до ответа кассы пустота
    // читалась как «за смену не пробито ничего».
    state.reading || state.busy -> ScreenState.Working
    state.shift == ShiftState.Open && !state.documentsRead ->
        ScreenState.Trouble(texts.documentsUnread, texts.documentsUnreadHint, onRetry)

    else -> ScreenState.Empty(
        icon = AppIcons.history,
        // Своё название, а не повтор заголовка над списком: два
        // одинаковых «Документы смены» подряд ничего не добавляли.
        title = texts.shiftEmpty,
        hint = emptyHint(state, texts)
    )
}

/**
 * Чем объясняется пустой список.
 *
 * Состояний смены три, и подсказка у каждого своя. Прежде их было две:
 * состояние, которого касса не назвала, объявлялось закрытой сменой —
 * при плитке «Смена: Неизвестна» прямо над списком, — а открытой смене
 * подсказкой доставалось одно слово «Чек».
 */
private fun emptyHint(state: DashboardUiState, texts: DashboardStrings): String = when (state.shift) {
    ShiftState.Open -> texts.shiftEmptyHint
    ShiftState.Closed -> texts.openShiftHint
    ShiftState.Unknown -> texts.shiftUnknownHint
}

@Composable
private fun DocumentRow(
    document: FiscalDocumentResponse,
    documentTitle: String,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    val texts = LocalStrings.current
    // Плашка доставки и код отказа стоят под названием, а не справа:
    // справа рядом с суммой и кнопками они оставляли названию и номеру
    // документа ноль точек на крупном шрифте.
    RecordRow(
        title = documentTitle,
        amount = documentAmount(document),
        support = { DocumentFacts(document) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

/** Номер документа, доставка и код отказа — переносятся, а не сжимаются. */
@Composable
private fun DocumentFacts(document: FiscalDocumentResponse) {
    val texts = LocalStrings.current
    WrapRow {
        // Номер подписан: голая «1» под словом «Продажа» читалась как
        // количество, а не как номер документа.
        Text(
            text = document.number?.let { "${texts.dashboard.documentNo} $it" } ?: Glyphs.DASH,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DocumentDeliveryChip(document)
        // Код отказа вместо кнопки повтора: документ, который ОФД
        // отверг, повторной отправкой не исправить — операцию нужно
        // провести заново. Кассиру полезен не повтор, а причина.
        document.refusalCode?.let { code ->
            Text(
                text = "${texts.common.refusalCode} $code",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
