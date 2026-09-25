package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.domain.kassa.model.refund.span
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
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.payment.SplitEditor
import kz.mybrain.superkassa.presentation.kassa.payment.fiscal
import kz.mybrain.superkassa.presentation.words.kassa.title
import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Возврат по чеку-основанию.
 *
 * Модель живёт, пока открыто окно: выбранный день, чек, набранная сумма
 * и ключ неудавшейся попытки переживают уход в другой раздел. Основание
 * ищется по дню, а не в открытой смене: покупатель приходит с чеком
 * позавчерашнего дня, и протокол этого не запрещает.
 */
class ReturnsViewModel(private val cases: RefundCases, private val talk: Talk) : ViewModel(), BasisActions {
    private val screen = MutableStateFlow(ReturnsUiState())
    private val busy = Busy()
    private val reading = latest()
    private val texts: CommonTexts get() = textsOf(talk.language()).common

    val state: StateFlow<ReturnsUiState> = screen.asStateFlow()

    val refund: RefundActions = RefundEditor(screen, busy, ::submit)
    val payments: PaymentActions = SplitEditor { change ->
        screen.update { now -> now.copy(refund = now.refund?.let { it.copy(split = change(it.split)) }) }
    }

    init {
        followSeat(
            cases.observe(),
            each = { signIn -> screen.update { it.copy(kkm = signIn.kkm, signedIn = signIn.signedIn) } },
            seated = ::reseat
        )
        follow(busy.active) { on -> screen.update { it.copy(working = on) } }
        viewModelScope.launch {
            val reference = cases.reference()
            screen.update { it.withReference(reference) }
        }
    }

    /**
     * Экран открыт: день перечитывается — чек могли пробить только что;
     * каналы доставки — их могли настроить в другом разделе.
     */
    fun visit() {
        rereadDay()
        viewModelScope.launch {
            val channels = cases.channels()
            screen.update { it.withChannels(channels) }
        }
    }

    override fun kind(kind: ReturnKind) = screen.update { it.copy(kind = kind, refund = null) }

    override fun day(day: LocalDate) {
        screen.update { it.copy(day = day, refund = null, documents = emptyList()) }
        rereadDay()
    }

    override fun number(text: String) = screen.update { it.copy(number = text.filter(Char::isDigit)) }

    /** Выбранный чек открывает возврат; строки чека касса отдаёт своим обращением. */
    override fun choose(basis: FiscalDocumentResponse) {
        if (busy.now) return
        screen.update { it.copy(refund = RefundDraft(basis)) }
        if (!screen.value.signedIn) return
        viewModelScope.launch {
            val items = cases.readItems(basis.id).shown(texts.returns.title, "read basis items", talk)
            screen.update { now ->
                now.copy(
                    refund = now.refund?.takeIf { it.basis.id == basis.id }?.copy(
                        items = items.orEmpty(),
                        itemsRead = items != null
                    )
                )
            }
        }
    }

    override fun back() = screen.update { if (busy.now) it else it.copy(refund = null) }

    override fun rereadDay() {
        reading.restart { read() }
    }

    /**
     * Оформляет возврат по выбранному чеку.
     *
     * Ключ живёт, пока возвращают то же самое; удался возврат — выбор
     * снимается: сумма чека в поле после частичного возврата приглашала бы
     * вернуть его ещё раз. После возврата день перечитывается: возврат по
     * только что возвращённому чеку должен видеть и его, и остаток ящика.
     */
    private fun submit() {
        val now = screen.value
        val draft = now.refund?.takeIf { now.canRefund && !busy.now && now.kkm != null } ?: return
        val lineName = refundLineName(texts.returns.refundFor, draft)
        val plan = draft.plan(now.kind, now.kkm?.kkmKgdId.orEmpty(), lineName, now.domainKind.plain)
        if (!plan.complete) return
        val attempt = draft.attemptFor(plan)
        screen.update { it.copy(refund = it.refund?.copy(attempt = attempt)) }
        whileBusy(busy) {
            val what = plan.kind.title(texts.returns)
            val done = textsOf(talk.language()).kassa.checkout.refundDone.fill(what, Money.formatTiyn(plan.refundTiyn))
            val words = FiscalWords(what, done, "refund")
            val outcome = talk.fiscal(cases.issue(plan, attempt.key), words, texts)
            screen.update { it.copy(refund = it.refund?.after(outcome)) }
            read()
        }
    }

    /** За кассой сел другой: день и выбранный чек принадлежали прежнему. */
    private fun reseat(signIn: SignInState) {
        reading.cancel()
        screen.update {
            ReturnsUiState(signIn.kkm, signIn.signedIn, channels = it.channels).withReference(it.reference)
        }
        rereadDay()
    }

    private suspend fun read() {
        val day = screen.value.day
        val seated = cases.observe().value
        screen.update { it.copy(loading = seated.signedIn) }
        if (!seated.signedIn) return
        val (from, to) = day.span()
        val read = cases.readDay(from, to)
        if (screen.value.day != day || !seated.sameSeat(cases.observe().value)) return
        screen.update { if (read == null) it.copy(loading = false) else it.adopt(read) }
        // Первая беда — словами кассы, если строка сообщений свободна: запертый
        // пин и отказ по пину не должны прятаться за пустым списком.
        read?.trouble?.takeIf { talk.silent }?.shown(texts.returns.title, "read return day", talk)
    }
}
