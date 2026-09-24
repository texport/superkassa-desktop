package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState

/*
 * Главный экран для снимков вида: состояние собрано руками, без модели.
 *
 * Доводы те же, что у сеанса снимков на узле: смена, документы, наличные,
 * роль кассира. Значения кассы — в оснастке проверок (`DashboardScene`),
 * состояние экрана знают только экраны, и собирается оно здесь.
 */

/**
 * Прочитанное: касса назвала смену и отдала её документы. Неизвестную
 * смену и непрочитанные документы снимок получает `copy`-ем — [unknown], [unread].
 *
 * @param shift открытая смена; `null` — смена закрыта.
 */
fun DashboardScene.state(
    kkm: KkmResponse? = CoreScene.kkm(),
    admin: Boolean = true,
    shift: ShiftResponse? = null,
    documents: List<FiscalDocumentResponse> = emptyList(),
    cash: Long? = CASH
) = DashboardUiState(
    kkm = kkm,
    isAdmin = admin,
    shift = if (shift == null) ShiftState.Closed else ShiftState.Open,
    shiftNumber = shift?.shiftNo,
    dayLimitAt = shift?.dayLimitAt,
    dayLimitExceeded = shift?.dayLimitExceeded == true,
    documents = documents,
    documentsRead = true,
    cashInDrawer = cash,
    documentTypes = TYPES
)

/** Касса о смене промолчала: о ней и о документах неизвестно ничего. */
fun DashboardUiState.unknown() = copy(shift = ShiftState.Unknown, documents = emptyList(), documentsRead = false)

/** Смену касса назвала, а документы отдать отказалась. */
fun DashboardUiState.unread() = copy(documents = emptyList(), documentsRead = false)
