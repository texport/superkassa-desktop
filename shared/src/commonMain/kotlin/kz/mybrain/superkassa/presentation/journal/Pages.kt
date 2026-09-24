package kz.mybrain.superkassa.presentation.journal

import kz.mybrain.superkassa.domain.journal.model.Paged

/**
 * Прочитанное и итог чтения после ответа кассы.
 *
 * Касса, которая не ответила, о том, есть ли за прочитанным ещё, не сказала
 * ничего: прочитанное остаётся на экране, и прежний ответ на этот вопрос —
 * в силе. Забыв его, список писал «показан весь срок» под сроком,
 * оборванным на странице.
 *
 * @receiver прочитанное вместе со страницей; `null` — страница не пришла.
 */
internal fun <T> Paged<T>?.outcome(shown: List<T>, previous: PageOutcome): Pair<List<T>, PageOutcome> =
    if (this == null) shown to PageOutcome.unreadAfter(previous) else items to PageOutcome.page(more)
