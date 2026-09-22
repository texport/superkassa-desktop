package kz.mybrain.superkassa.desktop.ui.returns

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.PageOutcome
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Что стоит на месте списка чеков-оснований.
 *
 * Пять случаев, и порядок между ними — порядок того, что кассир обязан
 * узнать раньше: блокировка кассы и закрытая смена запрещают возврат
 * целиком, и говорить о чеках дня при них незачем. Найденные чеки
 * показываются и во время дочитывания дня, а вот пустота и неудача
 * чтения разведены: узел, который не ответил, об отсутствии чека
 * покупателя ничего не сказал.
 */
internal fun basisState(
    session: Session,
    journal: ReturnJournalTexts,
    kind: ReturnKind,
    found: Boolean,
    loading: Boolean,
    page: PageOutcome,
    onRetry: () -> Unit
): ScreenState = when {
    // Заблокированная касса — и снятая с учёта в том числе — фискальных
    // команд не принимает, а смена у неё может оставаться открытой: без
    // этой проверки кассиру оставалась нажимаемая кнопка, на которую узел
    // отвечает KKM_BLOCKED.
    session.selected?.isBlocked == true ->
        ScreenState.Empty(AppIcons.noBasis, journal.kkmBlocked, journal.kkmBlockedHint)
    // Закрытая смена — состояние, а не отказ: об этом сказано словами
    // и подсказкой, а не пустым списком, из которого ничего не понять.
    !session.shiftOpen -> ScreenState.Empty(AppIcons.noBasis, journal.shiftClosed, journal.shiftClosedHint)
    found -> ScreenState.Ready
    loading -> ScreenState.Working
    !page.read -> ScreenState.Trouble(journal.basisUnread, journal.basisUnreadHint, onRetry)
    else -> ScreenState.Empty(AppIcons.noBasis, kind.emptyText(journal), journal.noBasisHint)
}
