package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse

/**
 * Документ, проведённый кассой: какой и что с ним стало в БФД.
 *
 * Касса отвечает на фискальную команду вместе с причиной отказа: кодом БФД
 * и словами на трёх языках. Дочитывать документ ради кода не нужно.
 *
 * @property refusalCode код, которым БФД отверг документ; `null` — не отверг
 *   или БФД не ответил.
 * @property refusal почему БФД отверг документ и что делать — словами кассы
 *   на трёх языках; `null` — не отверг.
 */
data class Fiscal(
    val documentId: String,
    val delivery: DeliveryStatus,
    val refusalCode: Int? = null,
    val refusal: TrilingualMessageResponse? = null
) {

    /** БФД отверг документ: фискального действия нет, и набранное исправляют. */
    val rejected: Boolean get() = delivery == DeliveryStatus.ONLINE_ERROR
}

/** Чек и его судьба в БФД со слов кассы. */
internal fun ReceiptResponse.fiscal(): Fiscal = Fiscal(documentId, deliveryStatus, bfdResultCode, deliveryError)

/** Внесение или изъятие и его судьба в БФД со слов кассы. */
internal fun CashOperationResponse.fiscal(): Fiscal = Fiscal(documentId, deliveryStatus, bfdResultCode, deliveryError)

/**
 * Чем кончилась фискальная операция для того, кто её набирал.
 *
 * Три исхода требуют трёх разных действий. Принятое забывается, и
 * следующая операция идёт со своим ключом. Отвергнутое БФД фискального
 * действия не имеет: набранное остаётся для исправления, а ключ
 * заменяется — с прежним касса отвечала бы тем же отвергнутым документом,
 * и операцию нельзя было бы провести никогда. Отказ кассы и неизвестный
 * исход оставляют и набранное, и ключ: повтор не проведёт операцию дважды.
 */
enum class FiscalOutcome { Accepted, Rejected, Unsettled }

/** Исход операции по ответу кассы. */
val Answer<Fiscal>.outcome: FiscalOutcome
    get() = when {
        this !is Answer.Done -> FiscalOutcome.Unsettled
        value.rejected -> FiscalOutcome.Rejected
        else -> FiscalOutcome.Accepted
    }
