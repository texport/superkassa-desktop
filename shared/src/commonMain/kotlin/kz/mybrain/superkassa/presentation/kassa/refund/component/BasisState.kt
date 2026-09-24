package kz.mybrain.superkassa.presentation.kassa.refund.component

import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.presentation.strings.journal.ReturnJournalTexts
import kz.mybrain.superkassa.presentation.strings.kassa.emptyText
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons

/**
 * Что стоит на месте списка чеков-оснований.
 *
 * Пять случаев, и порядок между ними — порядок того, что кассир обязан
 * узнать раньше: блокировка кассы и закрытая смена запрещают возврат
 * целиком, и говорить о чеках дня при них незачем. Найденные чеки
 * показываются и во время дочитывания дня, а вот пустота и неудача
 * чтения разведены: касса, которая не ответила, об отсутствии чека
 * покупателя ничего не сказала.
 */
internal fun basisState(state: ReturnsUiState, journal: ReturnJournalTexts, onRetry: () -> Unit): ScreenState = when {
    // Заблокированная касса — и снятая с учёта в том числе — фискальных
    // команд не принимает, а смена у неё может оставаться открытой: без
    // этой проверки кассиру оставалась нажимаемая кнопка, на которую касса
    // отвечает KKM_BLOCKED.
    state.blocked ->
        ScreenState.Empty(AppIcons.noBasis, journal.kkmBlocked, journal.kkmBlockedHint)
    // Закрытая смена — состояние, а не отказ: об этом сказано словами
    // и подсказкой, а не пустым списком, из которого ничего не понять.
    !state.shiftOpen -> ScreenState.Empty(AppIcons.noBasis, journal.shiftClosed, journal.shiftClosedHint)
    state.candidates.isNotEmpty() -> ScreenState.Ready
    state.loading -> ScreenState.Working
    !state.dayRead -> ScreenState.Trouble(journal.basisUnread, journal.basisUnreadHint, onRetry)
    else -> ScreenState.Empty(AppIcons.noBasis, state.kind.emptyText(journal), journal.noBasisHint)
}
