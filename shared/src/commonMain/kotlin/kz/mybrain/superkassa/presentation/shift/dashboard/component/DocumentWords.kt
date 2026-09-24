package kz.mybrain.superkassa.presentation.shift.dashboard.component

import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.document.model.hasOwnAmount
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

/** Название вида документа: из справочника кассы, иначе своими словами, иначе код. */
@Composable
internal fun documentTitle(state: DashboardUiState, docType: String): String =
    state.typeTitle(docType, LocalLanguage.current)
        ?: LocalStrings.current.enums.documentFallback(docType)
        ?: docType

/**
 * Сумма документа в строке смены.
 *
 * У отчёта и открытия смены своей суммы нет: касса держит у них ноль,
 * и «0,00 ₸» рядом с X-отчётом кассир читал как «не продано ничего».
 * Журнал за срок на том же месте ставит прочерк, а обе таблицы кассир
 * читает одинаково.
 */
internal fun documentAmount(document: FiscalDocumentResponse): String =
    if (document.hasOwnAmount) Money.formatTiyn(document.totalAmount) else Glyphs.DASH
