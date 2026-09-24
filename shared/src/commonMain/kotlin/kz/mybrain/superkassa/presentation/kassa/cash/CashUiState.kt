package kz.mybrain.superkassa.presentation.kassa.cash

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.cash.CashAttempt
import kz.mybrain.superkassa.domain.kassa.model.cash.CashDecision
import kz.mybrain.superkassa.domain.kassa.model.cash.CashHoldup
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.cash.CashRules
import kz.mybrain.superkassa.domain.kassa.model.cash.cashHoldup
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kkm.model.isBlocked

/**
 * Денежный ящик: остаток, набранная сумма, ждущее подтверждения движение
 * и что уже проведено за сутки.
 *
 * @property cashInDrawer наличные в ящике, в тиынах; `null` — касса не сказала.
 * @property amount сумма, как её набрал кассир.
 * @property attempt последняя попытка: её ключ живёт, пока деньги не проведены,
 *   и повтор той же суммы после неизвестного исхода уходит с ним же.
 * @property asked движение, о котором кассира спрашивают перед проведением.
 * @property busy движение проводится: окно не закрывается и не жмётся дважды.
 * @property recent внесения и изъятия за сутки, новые первыми.
 * @property recentRead касса ответила на список: пустой и непрочитанный — разные вещи.
 * @property recentLoading список читается.
 * @property documentTypes названия видов документов из справочника кассы.
 */
data class CashUiState(
    val kkm: KkmResponse? = null,
    val signedIn: Boolean = false,
    val shiftOpen: Boolean = false,
    val cashInDrawer: Long? = null,
    val amount: String = "",
    val attempt: CashAttempt? = null,
    val asked: CashAttempt? = null,
    val busy: Boolean = false,
    val recent: List<FiscalDocumentResponse> = emptyList(),
    val recentRead: Boolean = false,
    val recentLoading: Boolean = true,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap()
) {
    val blocked: Boolean get() = kkm?.isBlocked == true

    fun decision(move: CashMove): CashDecision = CashRules.check(amount, move, cashInDrawer, shiftOpen, blocked)

    /** Что мешает провести деньги; `null` — ничего, и под полем подсказка ввода. */
    val holdup: CashHoldup?
        get() = cashHoldup(decision(CashMove.Deposit), decision(CashMove.Withdraw), shiftOpen, blocked)

    /** Провести можно: касса выбрана, кассир вошёл и прежнее движение кончилось. */
    val ready: Boolean get() = kkm != null && signedIn && !busy

    /**
     * Ящик после ответа кассы. Проведённые деньги отпускают и сумму, и ключ:
     * следующая такая же сумма — уже другая операция. Отвергнутое БФД движение
     * оставляет сумму, но не ключ: с прежним касса ответила бы тем же отказом.
     * Отказ кассы и неизвестный исход оставляют обоих.
     */
    fun after(outcome: FiscalOutcome): CashUiState = when (outcome) {
        FiscalOutcome.Accepted -> copy(amount = "", attempt = null, asked = null)
        FiscalOutcome.Rejected -> copy(attempt = null, asked = null)
        FiscalOutcome.Unsettled -> copy(asked = null)
    }
}

/** Что кассир делает с ящиком. По умолчанию пусто — для снимков вида. */
interface CashActions {
    fun enter(amount: String) = Unit

    /** Спросить подтверждение внесения или изъятия набранной суммы. */
    fun ask(move: CashMove) = Unit

    fun cancel() = Unit

    fun confirm() = Unit

    fun rereadRecent() = Unit
}
