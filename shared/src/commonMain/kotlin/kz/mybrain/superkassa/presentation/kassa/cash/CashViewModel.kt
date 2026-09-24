package kz.mybrain.superkassa.presentation.kassa.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.cash.CashAttempt
import kz.mybrain.superkassa.domain.kassa.model.cash.CashDecision
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.cash.CashRules
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.domain.signin.model.sameSeat
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.kassa.payment.FiscalWords
import kz.mybrain.superkassa.presentation.kassa.payment.fiscal
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kotlin.time.Clock

/**
 * Внесение и изъятие наличных.
 *
 * Модель живёт, пока открыто окно: набранная сумма и ключ неудавшейся
 * попытки переживают уход в другой раздел. Подтверждение стоит между
 * вводом и документом: изъятие тысячи и десяти тысяч отличаются одним
 * нажатием, а проведённое движение не отменить.
 */
class CashViewModel(private val cases: CashCases, private val talk: Talk) : ViewModel(), CashActions {
    private val screen = MutableStateFlow(CashUiState())
    private val busy = Busy()
    private val reading = latest()
    private val texts: AppStrings get() = stringsOf(talk.language())

    val state: StateFlow<CashUiState> = screen.asStateFlow()

    init {
        followSeat(
            cases.observe(),
            each = { signIn -> screen.update { it.copy(kkm = signIn.kkm, signedIn = signIn.signedIn) } },
            seated = ::reseat
        )
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
        viewModelScope.launch {
            cases.documentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    /** Экран открыт: остаток, смена и список за сутки перечитываются. */
    fun visit() {
        reading.restart { read() }
    }

    override fun enter(amount: String) = screen.update { it.copy(amount = amount) }

    /** Та же сумма тем же движением идёт с прежним ключом: касса не проведёт её дважды. */
    override fun ask(move: CashMove) = screen.update { now ->
        val amount = (now.decision(move) as? CashDecision.Ready)?.amount ?: return@update now
        val attempt = CashRules.attemptFor(now.attempt, move, amount)
        now.copy(attempt = attempt, asked = attempt)
    }

    override fun cancel() = screen.update { if (it.busy || busy.now) it else it.copy(asked = null) }

    override fun confirm() {
        val pending = screen.value.asked?.takeIf { !screen.value.busy }
        if (pending == null || screen.value.kkm == null) return
        whileBusy(busy) {
            val outcome = move(pending)
            screen.update { it.after(outcome) }
            read()
        }
    }

    override fun rereadRecent() {
        reading.restart { read() }
    }

    /** Движение — в кассу; итог назван кассиру. */
    private suspend fun move(pending: CashAttempt): FiscalOutcome {
        val words = texts.cash
        val deposit = pending.move == CashMove.Deposit
        val done = "${if (deposit) words.deposited else words.withdrawn} ${Money.formatTiyn(pending.amount)}"
        val fiscal = FiscalWords(if (deposit) words.deposit else words.withdraw, done, "cash move")
        return talk.fiscal(cases.move(pending), fiscal, texts)
    }

    /** За кассой сел другой: набранное и прочитанное принадлежали прежнему. */
    private fun reseat(signIn: SignInState) {
        reading.cancel()
        screen.update { CashUiState(signIn.kkm, signIn.signedIn, documentTypes = it.documentTypes, busy = busy.now) }
        reading.restart { read() }
    }

    /** Ящик, прочитанный для прежнего кассира, новому не показывается. */
    private suspend fun read() {
        val seated = cases.observe().value
        val kkm = seated.kkm?.takeIf { seated.signedIn } ?: return
        screen.update { it.copy(recentLoading = true) }
        val drawer = cases.readDrawer(Clock.System.now().toEpochMilliseconds())
            ?.takeIf { seated.sameSeat(cases.observe().value) } ?: return
        screen.update {
            it.copy(
                shiftOpen = drawer.shiftOpen ?: kkm.isShiftOpen,
                cashInDrawer = drawer.cash,
                recent = drawer.recent ?: it.recent,
                recentRead = drawer.recent != null,
                recentLoading = false
            )
        }
        drawer.trouble?.takeIf { talk.silent }?.shown(texts.cash.deposit, "read drawer", talk)
    }
}
