package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.CounterSnapshotResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.DocumentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.presentation.dashboard.DashboardUiState

/**
 * Главный экран для снимков вида: состояние собрано руками, без модели.
 *
 * Доводы те же, что у сеанса снимков на узле: смена, документы, наличные,
 * роль кассира. Экран рисует готовое состояние, и снимку незачем
 * поднимать кассу ради картинки.
 */
object DashboardScene {

    /**
     * Прочитанное: касса назвала смену и отдала её документы. Неизвестную
     * смену и непрочитанные документы снимок получает `copy`-ем — [unknown], [unread].
     *
     * @param shift открытая смена; `null` — смена закрыта.
     */
    fun state(
        kkm: KkmResponse? = CoreScene.kkm(),
        admin: Boolean = true,
        shift: ShiftResponse? = null,
        documents: List<Document> = emptyList(),
        cash: Long? = CASH
    ) = DashboardUiState(
        kkm = kkm,
        isAdmin = admin,
        shift = if (shift == null) ShiftState.Closed else ShiftState.Open,
        shiftNumber = shift?.shiftNo,
        shiftOpenedAt = shift?.openedAt,
        documents = documents.map(::core),
        documentsRead = true,
        cashInDrawer = cash,
        documentTypes = TYPES
    )

    /** Касса о смене промолчала: о ней и о документах неизвестно ничего. */
    fun DashboardUiState.unknown() = copy(shift = ShiftState.Unknown, documents = emptyList(), documentsRead = false)

    /** Смену касса назвала, а документы отдать отказалась. */
    fun DashboardUiState.unread() = copy(documents = emptyList(), documentsRead = false)

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

    /** Документ снимка в типах ядра: те же поля, что у документа узла. */
    fun core(document: Document) = FiscalDocumentResponse(
        id = document.id,
        cashboxId = "kkm-1",
        shiftId = "shift-7",
        docType = document.docType.orEmpty(),
        docNo = document.docNo,
        printedDocumentNumber = document.printedDocumentNumber,
        shiftNo = document.shiftNo?.toLong(),
        createdAt = document.createdAt ?: 0,
        totalAmount = document.totalAmount,
        currency = "KZT",
        fiscalSign = document.fiscalSign,
        autonomousSign = document.autonomousSign,
        isAutonomous = document.isAutonomous == true,
        ofdStatus = document.ofdStatus,
        ofdErrorCode = document.ofdErrorCode,
        ofdErrorText = document.ofdErrorText,
        deliveredAt = document.deliveredAt
    )

    /** Наличные в ящике снимка, в тиынах. */
    private const val CASH = 125_000L

    /** Справочник видов документов, как его отдаёт касса; в снимке — по-русски. */
    private val TYPES = listOf(
        "SALE" to "Продажа",
        "RETURN" to "Возврат продажи",
        "CASH_IN" to "Внесение",
        "CASH_OUT" to "Изъятие",
        "X_REPORT" to "X-отчёт",
        "SHIFT_OPEN" to "Открытие смены"
    ).associate { (code, ru) -> code to TrilingualMessageResponse(ru, ru, ru) }
}
