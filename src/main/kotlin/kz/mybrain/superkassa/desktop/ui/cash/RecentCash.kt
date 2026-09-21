package kz.mybrain.superkassa.desktop.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.documents
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.DrawerTexts
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Что уже внесено и изъято.
 *
 * Список берётся из журнала за сутки, а не из документов смены: смену
 * закрывают в конце дня, и после закрытия кассир всё равно должен видеть,
 * куда ушли деньги из ящика.
 *
 * Заголовок вынесен над карточкой: так список читается как раздел экрана,
 * а не как ещё одна карточка с непонятно чем внутри.
 */
@Composable
internal fun RecentCash(session: Session, money: DrawerTexts, recent: List<Document>, loading: Boolean) {
    val state = when {
        recent.isNotEmpty() -> ScreenState.Ready
        loading -> ScreenState.Working
        else -> ScreenState.Empty(AppIcons.cash, money.recentEmpty, money.recentEmptyHint)
    }
    SectionCard(title = money.recent, info = money.recentHint) {
        ScreenSlot(state, dense = true) {
            recent.forEachIndexed { index, document ->
                if (index > 0) {
                    HorizontalDivider()
                }
                CashRow(session, document)
            }
        }
    }
}

/**
 * Строка движения наличных.
 *
 * Сумма стоит справа моноширинно и одной ширины у всех строк: столбец
 * читается сверху вниз одним движением глаза. Плашка доставки ушла к
 * времени операции — окажись она рядом с суммой, суммы разъехались бы
 * по ширине слова «доставлено».
 */
@Composable
private fun CashRow(session: Session, document: Document) {
    val paidIn = document.docType == CASH_IN
    RecordRow(
        title = session.titleOf(Dictionary.DocumentTypes, document.docType),
        amount = Money.formatTiyn(document.totalAmount),
        leading = {
            Icon(
                imageVector = if (paidIn) AppIcons.paidIn else AppIcons.paidOut,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        support = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(moment(document.createdAt), style = MaterialTheme.typography.bodySmall)
                DeliveryChip(document.ofdStatus, document.isAutonomous == true)
            }
        },
    )
}

/**
 * Перечитывает движения наличных за сутки.
 *
 * Вызывается и при входе на экран, и после проведения: кассир должен
 * увидеть только что внесённые деньги в списке, а не гадать, прошли ли они.
 */
internal suspend fun reloadRecent(session: Session, money: MoneyTexts, into: MutableList<Document>) {
    val kkm = session.selected ?: return
    val now = System.currentTimeMillis()
    val loaded = session.guard(money.drawer.recent) {
        session.client.documents(kkm.kkmId, now - DAY_MILLIS, now, session.pin)
    } ?: return
    into.clear()
    into.addAll(loaded.filter { it.docType in CASH_TYPES }.take(RECENT_LIMIT))
}

private fun moment(millis: Long?): String {
    val value = millis ?: return Glyphs.DASH
    return FORMAT.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
}

private const val CASH_IN = "CASH_IN"

/** Виды документов, которыми узел записывает движение наличных. */
private val CASH_TYPES = setOf(CASH_IN, "CASH_OUT")

/** Сколько операций показывать: список под формой, а не журнал. */
private const val RECENT_LIMIT = 10

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
