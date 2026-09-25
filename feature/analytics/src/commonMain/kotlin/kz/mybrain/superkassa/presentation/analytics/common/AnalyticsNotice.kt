package kz.mybrain.superkassa.presentation.analytics.common

import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Помеха аналитики словами владельца — в общем виде состояний экрана.
 *
 * Своего ожидания и своей карточки отказа у раздела больше нет: кружок
 * с подписью и объяснение с кнопкой повтора одни на всё приложение
 * и живут в `ScreenSlot`. Здесь остаётся только то, чего нет больше
 * нигде, — свой разбор причин аналитики.
 */
internal fun analyticsTroubleState(
    trouble: AnalyticsTrouble,
    texts: AnalyticsTexts,
    onRetry: () -> Unit
): ScreenState.Trouble = ScreenState.Trouble(
    title = troubleTitle(trouble, texts),
    hint = troubleHint(trouble, texts),
    onRetry = onRetry
)

/**
 * Что стоит на месте вкладки по ответу кабинета.
 *
 * Помеха и ожидание решаются одинаково на всех вкладках; своё у вкладки —
 * только пустота: пустой кабинет, пустой срок, пустой список адресов.
 *
 * @param empty пустота пришедшего словами вкладки; `null` — показывать есть что.
 */
internal fun <T> analyticsScreenState(
    reading: Reading<T>,
    texts: AnalyticsTexts,
    onRetry: () -> Unit,
    empty: (T) -> ScreenState.Empty?
): ScreenState {
    val trouble = reading.trouble
    val value = reading.value
    return when {
        trouble != null -> analyticsTroubleState(trouble, texts, onRetry)
        value == null -> ScreenState.Working
        else -> empty(value) ?: ScreenState.Ready
    }
}
