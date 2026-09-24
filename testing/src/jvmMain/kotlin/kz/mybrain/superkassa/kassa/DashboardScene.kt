package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.CounterSnapshotResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.DocumentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse

/**
 * Касса главного экрана: смена, документы, наличные и справочник видов
 * документов — одни и те же для сценариев смены и снимков экрана.
 *
 * Состояние экрана из тех же значений собирают проверки экранов
 * (`DashboardScene.state`): экран рисует готовое состояние, и снимку
 * незачем поднимать кассу ради картинки.
 */
object DashboardScene {

    /**
     * Касса, которая отвечает главному экрану: касса, смена, документы, наличные.
     *
     * @param shift открытая смена; `null` — смена закрыта.
     */
    fun core(
        kkm: KkmResponse = CoreScene.kkm(),
        shift: ShiftResponse? = CoreScene.openShift(),
        documents: List<FiscalDocumentResponse> = emptyList(),
        cash: Long = CASH
    ): FakeCore = FakeCore().apply {
        on("getKkm") { kkm }
        on("getLocalOpenShift") { shift }
        on("listShiftDocuments") { documents }
        on("listCounters") {
            listOf(CounterSnapshotResponse(scope = "GLOBAL", key = "cash.sum", value = cash, updatedAt = 0))
        }
        on("getDocumentTypes") { TYPES.map { (code, name) -> DocumentTypeResponse(code, name) } }
    }

    /** Наличные в ящике снимка, в тиынах. */
    const val CASH = 125_000L

    /** Справочник видов документов, как его отдаёт касса; в снимке — по-русски. */
    val TYPES = listOf(
        "SALE" to "Продажа",
        "RETURN" to "Возврат продажи",
        "CASH_IN" to "Внесение",
        "CASH_OUT" to "Изъятие",
        "X_REPORT" to "X-отчёт",
        "SHIFT_OPEN" to "Открытие смены"
    ).associate { (code, ru) -> code to TrilingualMessageResponse(ru, ru, ru) }
}
