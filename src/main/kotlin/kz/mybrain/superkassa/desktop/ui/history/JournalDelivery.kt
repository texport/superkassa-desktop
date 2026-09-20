package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.StatusStrings
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Что стало с документом по дороге в ОФД.
 *
 * Узел и кабинет называют одно и то же разными кодами — `SENT` против
 * `ONLINE_OK`, `FAILED` против `DELIVERY_ERROR`. Отбор по состоянию
 * обязан работать на обоих экранах одинаково, поэтому коды приводятся
 * к этому перечислению у источника, а показ и отбор знают только его.
 *
 * Незнакомый код не становится состоянием: на экране кассы и владельца
 * протокольных кодов быть не должно, а угадывать смысл кода — значит
 * однажды покрасить отказ зелёным.
 */
enum class JournalDelivery(val title: (StatusStrings) -> String) {
    /** Принят ОФД. */
    Delivered({ it.delivered }),

    /** Пробит без связи и доставлен позже. */
    Resent({ it.resent }),

    /** Ждёт отправки. */
    Queued({ it.queued }),

    /** Отвергнут ОФД: фискальным документом не является. */
    Refused({ it.refused }),

    /** В ОФД не уходит: открытие смены такой командой протокол не знает. */
    Internal({ it.internal })
}

/** Цвет состояния: сделано, ожидание или отказ — как везде в приложении. */
@Composable
fun JournalDelivery.color(): Color = when (this) {
    JournalDelivery.Delivered, JournalDelivery.Resent -> StatusColors.delivered
    JournalDelivery.Queued -> StatusColors.pending
    JournalDelivery.Refused -> StatusColors.refused
    // Внутреннее не ждёт ничего и ни о чём не отчитывается: цвет ему
    // отводится нейтральный, иначе строка обещает ожидание ответа ОФД.
    JournalDelivery.Internal -> MaterialTheme.colorScheme.outline
}

/** Плашка состояния доставки; у записи без состояния её нет вовсе. */
@Composable
fun JournalDeliveryChip(delivery: JournalDelivery?) {
    val state = delivery ?: return
    Chip(text = state.title(LocalStrings.current.status), color = state.color())
}

/** Плашка состояния записи: те же два цвета, что и у доставки. */
@Composable
fun JournalStateChip(state: JournalState?) {
    val shown = state ?: return
    Chip(
        text = shown.title,
        color = if (shown.done) StatusColors.delivered else StatusColors.pending
    )
}
