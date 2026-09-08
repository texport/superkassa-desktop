package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Состояние документа одним словом и цветом.
 *
 * Слова взяты из речи кассира, а не из протокола: он не знает, что такое
 * SENT, но знает «доставлен».
 */
@Composable
fun DeliveryChip(status: String?, autonomous: Boolean = false, documentType: String? = null) {
    val texts = LocalStrings.current.status
    val (text, color) = when {
        // Открытие смены в ОФД не уходит никогда: команды COMMAND_OPEN_SHIFT
        // в протоколе нет. Прежние записи хранят у него состояние доставки,
        // но кассиру оно всё равно ничего не обещает.
        documentType == SHIFT_OPEN -> texts.internal to MaterialTheme.colorScheme.outline
        autonomous && status == SENT -> texts.resent to StatusColors.delivered
        status == SENT -> texts.delivered to StatusColors.delivered
        status == FAILED -> texts.refused to StatusColors.refused
        status == INTERNAL -> texts.internal to MaterialTheme.colorScheme.outline
        status == PENDING -> texts.queued to StatusColors.pending
        else -> (status ?: "—") to MaterialTheme.colorScheme.outline
    }
    Chip(text, color)
}

/**
 * Плашка состояния.
 *
 * Собрана из ролей схемы: цвет роли на её же контейнере. Material для
 * такого случая предлагает `AssistChip`, но он рассчитан на нажатие
 * и тянет за собой обводку и высоту кнопки — здесь же надпись, которую
 * читают, а не нажимают.
 */
@Composable
fun Chip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = CHIP_TINT))
            .padding(horizontal = Spacing.tight, vertical = Spacing.hairline)
    )
}

/** Насколько цвет плашки разбавлен фоном. */
private const val CHIP_TINT = 0.16f

/** Документ принят ОФД. */
const val SENT = "SENT"

private const val FAILED = "FAILED"
private const val PENDING = "PENDING"

/** Открытие смены: документ есть, команды в протоколе нет. */
const val SHIFT_OPEN = "SHIFT_OPEN"

/**
 * Документ, которого в протоколе нет.
 *
 * Открытие смены кассир и администратор видят в журнале, но в ОФД оно
 * не уходит: команда COMMAND_OPEN_SHIFT из протокола исключена.
 */
const val INTERNAL = "INTERNAL"
