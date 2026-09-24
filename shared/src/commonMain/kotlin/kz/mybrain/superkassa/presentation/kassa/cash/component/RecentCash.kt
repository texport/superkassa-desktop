package kz.mybrain.superkassa.presentation.kassa.cash.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.presentation.common.document.DocumentDeliveryChip
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.kassa.cash.CashUiState
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.common.of
import kz.mybrain.superkassa.presentation.strings.kassa.DrawerTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Что уже внесено и изъято.
 *
 * Список берётся из журнала за сутки, а не из документов смены: смену
 * закрывают в конце дня, и после закрытия кассир всё равно должен видеть,
 * куда ушли деньги из ящика.
 *
 * Заголовок вынесен над карточкой: так список читается как раздел экрана,
 * а не как ещё одна карточка с непонятно чем внутри.
 *
 * Пустой список и непрочитанный разведены: «внесений и изъятий не было»
 * над молчащей кассой отправляло кассира сводить ящик с выдуманной пустотой.
 */
@Composable
internal fun RecentCash(state: CashUiState, money: DrawerTexts, onRetry: () -> Unit) {
    val shown = when {
        state.recent.isNotEmpty() -> ScreenState.Ready
        state.recentLoading -> ScreenState.Working
        !state.recentRead -> ScreenState.Trouble(money.recentUnread, money.recentUnreadHint, onRetry)
        else -> ScreenState.Empty(AppIcons.cash, money.recentEmpty, money.recentEmptyHint)
    }
    SectionCard(title = money.recent, info = money.recentHint) {
        ScreenSlot(shown, dense = true) {
            // Строки стоят вплотную через черту, без зазора карточки между
            // ними: зазор сверху и снизу черты растягивал каждое движение
            // на треть выше строки списка.
            Column {
                state.recent.forEachIndexed { index, document ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    CashRow(document, state.documentTypes[document.docType])
                }
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
private fun CashRow(document: FiscalDocumentResponse, type: TrilingualMessageResponse?) {
    val paidIn = document.docType == CASH_IN
    val texts = LocalStrings.current
    RecordRow(
        title = type?.of(LocalLanguage.current) ?: if (paidIn) texts.cash.deposited else texts.cash.withdrawn,
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Dates.shortMoment(document.createdAt), style = MaterialTheme.typography.bodySmall)
                DocumentDeliveryChip(document)
            }
        },
    )
}

/** Вид документа внесения: по нему строка рисует стрелку внутрь. */
private const val CASH_IN = "CASH_IN"
