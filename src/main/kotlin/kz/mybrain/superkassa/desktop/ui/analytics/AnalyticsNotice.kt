package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts

/**
 * Помеха аналитики словами владельца — в общем виде состояний экрана.
 *
 * Своего ожидания и своей карточки отказа у раздела больше нет: кружок
 * с подписью и объяснение с кнопкой повтора одни на всё приложение
 * и живут в `ScreenSlot`. Здесь остаётся только то, чего нет больше
 * нигде, — свой разбор причин аналитики.
 */
fun analyticsTroubleState(
    trouble: AnalyticsTrouble,
    texts: AnalyticsTexts,
    onRetry: () -> Unit
): ScreenState.Trouble = ScreenState.Trouble(
    title = troubleTitle(trouble, texts),
    hint = troubleHint(trouble, texts),
    onRetry = onRetry
)
