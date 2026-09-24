package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.defaultVatOf
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.domain.signin.model.sameSeat
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.kassa.payment.FiscalWords
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.payment.SplitEditor
import kz.mybrain.superkassa.presentation.kassa.payment.fiscal
import kz.mybrain.superkassa.presentation.kassa.sale.entry.EntryEditor
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.kassa.title

/**
 * Продажа и покупка: корзина, набранное поверх неё и пробитие чека.
 *
 * Модель живёт, пока открыто окно: корзина, формы и ключ попытки чека
 * переживают уход кассира в другой раздел. Повтор после неизвестного
 * исхода идёт с тем же ключом, и касса не пробивает второй чек.
 * Сменились касса или кассир — чек начинается заново.
 */
class SaleViewModel(private val cases: SaleCases, private val talk: Talk) : ViewModel() {
    private val screen = MutableStateFlow(SaleUiState(collapsed = panelsOf(cases.panels())))
    private val busy = Busy()
    private val reading = latest()
    private val texts: AppStrings get() = stringsOf(talk.language())

    val state: StateFlow<SaleUiState> = screen.asStateFlow()

    val basket: BasketActions = BasketEditor(screen)
    val form: FormActions = FormEditor(screen)
    val entry: EntryActions = EntryEditor(viewModelScope, screen, cases.lookup, talk)
    val payments: PaymentActions = SplitEditor { change ->
        screen.update { if (it.issuing) it else it.copy(form = it.form.copy(split = change(it.form.split))) }
    }

    init {
        followSeat(
            cases.observe(),
            each = { signIn -> screen.update { it.copy(kkm = signIn.kkm, signedIn = signIn.signedIn) } },
            seated = ::reseat
        )
        follow(busy.active) { on -> screen.update { it.copy(issuing = on) } }
        viewModelScope.launch { readReference() }
    }

    /**
     * Экран открыт: смену, отрасль, свёрнутые разделы и каналы доставки
     * меняют в других разделах, и они перечитываются. Корзина остаётся как была.
     */
    fun visit() {
        screen.update { it.copy(collapsed = panelsOf(cases.panels())).withEntryOpen() }
        reading.restart { readSeat() }
        viewModelScope.launch { readChannels() }
    }

    /** Сворачивает раздел кассовой колонки; выбор помнится между запусками. */
    fun togglePanel(panel: SalePanel) {
        screen.update { it.copy(collapsed = if (panel in it.collapsed) it.collapsed - panel else it.collapsed + panel) }
        cases.rememberPanels(screen.value.collapsed.map { it.name }.toSet())
    }

    /**
     * Пробивает чек.
     *
     * Кнопка занята, пока касса отвечает, и чек не правится: пробивается
     * ровно то, что кассир видел, нажимая кнопку. Принятый чек очищает
     * корзину и даёт следующему чеку свой ключ; отказ и неизвестный исход
     * оставляют всё как было — с тем же ключом. Чек, отвергнутый БФД,
     * остаётся для исправления, но со своим новым ключом.
     */
    fun issue() {
        val now = screen.value
        if (busy.now || now.block != null || now.kkm == null) return
        screen.update { it.copy(issuing = true) }
        whileBusy(busy) {
            val answer = cases.issue(now.receipt)
            val outcome = said(now, answer)
            screen.update { it.after(outcome).withIssued(now.receipt, answer) }
            readSeat()
        }
    }

    /** Итог чека назван кассиру. */
    private fun said(state: SaleUiState, answer: Answer<Fiscal>): FiscalOutcome {
        val title = state.form.operation.title(texts.sale)
        val words = FiscalWords(what = title, done = title, action = "issue receipt")
        return talk.fiscal(answer, words, texts)
    }

    /** Итог пробитого чека прочитан: кассир начинает следующий. */
    fun nextReceipt() = screen.update { it.copy(issued = null) }

    /** За кассой сел другой: чек прежнего кассира новому не достаётся. */
    private fun reseat(signIn: SignInState) {
        reading.cancel()
        screen.update {
            SaleUiState(
                kkm = signIn.kkm,
                signedIn = signIn.signedIn,
                shiftOpen = signIn.kkm?.isShiftOpen == true,
                paymentTypes = it.paymentTypes,
                vatRates = it.vatRates,
                issuing = busy.now,
                collapsed = it.collapsed,
                channels = it.channels
            )
        }
        reading.restart { readSeat() }
    }

    /** Смена, отрасль и ставка новой позиции выбранной кассы. */
    private suspend fun readSeat() {
        val seated = cases.observe().value
        val kkm = seated.kkm?.takeIf { seated.signedIn } ?: return
        val seat = cases.seat(kkm)
        if (!seated.sameSeat(cases.observe().value)) return
        screen.update {
            it.copy(shiftOpen = seat.shiftOpen, domainKind = DomainKind.byCode(seat.domainCode)).withDefaultVat()
        }
    }

    /** Какими видами контакта можно отправить чек: молча, без них чек не отправляется. */
    private suspend fun readChannels() {
        val read = cases.channels()
        screen.update { it.withChannels(read) }
    }

    /** Справочники кассы: виды оплаты и ставки. Молча: без них экран берёт свои. */
    private suspend fun readReference() {
        val read = cases.reference()
        screen.update { it.copy(paymentTypes = read.paymentTypes, vatRates = read.vatRates).withDefaultVat() }
    }
}

/** Ставка кассы у ещё не начатой позиции: начатую кассир уже правит сам. */
private fun SaleUiState.withDefaultVat(): SaleUiState =
    if (draft.started) this else copy(draft = draft.copy(vatGroup = defaultVatOf(kkm, vatRates)))

/** Свёрнутые разделы из памяти рабочего места; незнакомые имена забываются. */
private fun panelsOf(names: Set<String>): Set<SalePanel> =
    SalePanel.entries.filter { it.name in names }.toSet()
