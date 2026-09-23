package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kz.mybrain.superkassa.desktop.server.DeliveryCode
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.SHIFT_OPEN
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.Tip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.StatusStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
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
    Internal({ it.internal }),

    /**
     * Код состояния пришёл, а разобрать его нечем.
     *
     * Названо словами, а не кодом: поле состояния в спецификации узла —
     * свободная строка, и в строке документа смены у кассира стоял
     * протокольный код. Угадывать смысл незнакомого кода нельзя —
     * так однажды покрасишь отказ зелёным, — но и молчать о состоянии
     * документа не следует.
     */
    Unknown({ it.unknown })
}

/** Цвет состояния: сделано, ожидание или отказ — как везде в приложении. */
@Composable
fun JournalDelivery.color(): Color = when (this) {
    JournalDelivery.Delivered, JournalDelivery.Resent -> StatusColors.delivered
    JournalDelivery.Queued -> StatusColors.pending
    JournalDelivery.Refused -> StatusColors.refused
    // Внутреннее не ждёт ничего и ни о чём не отчитывается: цвет ему
    // отводится нейтральный, иначе строка обещает ожидание ответа ОФД.
    JournalDelivery.Internal, JournalDelivery.Unknown -> MaterialTheme.colorScheme.outline
}

/**
 * Состояние доставки документа узла — одно на журнал и на записи списков.
 *
 * Разбираются оба набора кодов узла, см. [DeliveryCode]. Незнакомый код
 * состоянием по существу не становится: он доходит до экрана
 * [JournalDelivery.Unknown], то есть словами, а не кодом.
 *
 * @return `null`, когда узел о доставке не сказал ничего: состояния нет,
 *   и плашки у такого документа тоже нет.
 */
fun deliveryOf(document: Document): JournalDelivery? {
    val status = document.ofdStatus?.takeIf { it.isNotBlank() }
    return when {
        // Открытие смены в ОФД не уходит никогда: команды COMMAND_OPEN_SHIFT
        // в протоколе нет. Прежние записи хранят у него состояние доставки,
        // но кассиру оно всё равно ничего не обещает.
        document.docType == SHIFT_OPEN -> JournalDelivery.Internal
        status == null -> null
        status in DeliveryCode.internal -> JournalDelivery.Internal
        status in DeliveryCode.delivered ->
            if (document.isAutonomous == true) JournalDelivery.Resent else JournalDelivery.Delivered

        status in DeliveryCode.refused -> JournalDelivery.Refused
        status in DeliveryCode.queued -> JournalDelivery.Queued
        else -> JournalDelivery.Unknown
    }
}

/**
 * Плашка состояния документа в записи списка.
 *
 * Отличается от табличной ступенью шрифта: в записи списка плашка идёт
 * наравне с её подписями, а в ряду таблицы — наравне с клетками.
 */
@Composable
fun DocumentDeliveryChip(document: Document) {
    val state = deliveryOf(document)
    if (state == null) {
        Chip(Glyphs.DASH, MaterialTheme.colorScheme.outline)
        return
    }
    Chip(state.title(LocalStrings.current.status), state.color())
}

/**
 * Плашка состояния доставки; у записи без состояния её нет вовсе.
 *
 * У отказа под плашкой лежит причина: в строке журнала кассир видел одно
 * красное слово «Отклонён», а ни просмотра, ни печати у такого документа
 * нет — узнать, что произошло, было негде. Слова причины приходят от
 * источника, и подсказки нет у того, кто её не даёт.
 *
 * @param reason почему документ отвергнут: слова кассира и код отказа.
 */
@Composable
fun JournalDeliveryChip(delivery: JournalDelivery?, reason: String? = null) {
    val state = delivery ?: return
    val chip = @Composable {
        Chip(
            text = state.title(LocalStrings.current.status),
            color = state.color(),
            style = ROW_LABEL
        )
    }
    if (reason.isNullOrBlank()) chip() else Tip(reason) { chip() }
}

/** Плашка состояния записи: те же два цвета, что и у доставки. */
@Composable
fun JournalStateChip(state: JournalState?) {
    val shown = state ?: return
    Chip(
        text = shown.title,
        color = if (shown.done) StatusColors.delivered else StatusColors.pending,
        style = ROW_LABEL
    )
}

/**
 * Ступень шрифта плашки в строке таблицы.
 *
 * Та же, что у подписей столбцов: плашка крупнее ряда на крупном шрифте
 * разрывала слово «Доставлен» пополам, пока соседние столбцы стояли
 * свободно.
 */
private val ROW_LABEL: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmall
