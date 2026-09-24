package kz.mybrain.superkassa.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.setup.model.EnrollOutcome
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.domain.signin.model.Pin
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetCalls
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Подключение кассы: от заводского номера до входа кассира.
 *
 * Кассу нельзя завести ни с одного конца отдельно: касса процесса заводит
 * её по идентификатору и токену, которые выдаёт БФД, БФД — по заводскому
 * номеру, который выдаёт касса, а войти можно только пином администратора,
 * которого до заведения нет. Поэтому мастер идёт через обе службы подряд
 * и переносит числа между ними сам.
 *
 * Вход по ЭЦП и заведение кассы в кабинете ведёт кабинет окна, заявление
 * о постановке на учёт — своя модель шага; здесь — пройденное, заводской
 * номер и заведение кассы. Токен кабинета выпускается через [calls]:
 * помехи кабинета владелец видит одними словами везде.
 */
class SetupViewModel(
    private val cases: SetupCases,
    private val talk: Talk,
    private val calls: CabinetCalls
) : ViewModel(), SetupActions {
    private val screen = MutableStateFlow(SetupUiState(way = cases.firstWay, draft = cases.remember.read()))

    val state: StateFlow<SetupUiState> = screen.asStateFlow()

    /** Перечитывает контуры и кассы рабочего места: справочник мог не прочитаться прежде. */
    override fun reload() {
        viewModelScope.launch {
            val (contours, kkms) = cases.readContext()
            screen.update { it.copy(contours = contours ?: it.contours, kkms = kkms ?: it.kkms) }
        }
    }

    override fun chooseWay(way: SetupWay) = screen.update { it.copy(way = way) }

    override fun askStartOver(ask: Boolean) =
        screen.update { it.copy(startingOver = ask && it.draft.factoryNumber != null) }

    /** Забывает пройденное: заводской номер и касса в кабинете там и остаются. */
    override fun startOver() = show(cases.remember(KkmSetupDraft()))

    /** Выдаёт заводской номер; выданный второй раз не спрашивается. */
    override fun getFactory() {
        if (screen.value.gettingFactory) return
        screen.update { it.copy(gettingFactory = true) }
        viewModelScope.launch {
            val what = textsOf(talk.language()).setup.stepFactory
            val draft = cases.getFactoryNumber(screen.value.draft)?.shown(what, "factory number", talk)
            screen.update { it.copy(gettingFactory = false) }
            draft?.let(::show)
        }
    }

    override fun rememberRegister(id: String, kkmId: Int, name: String?) =
        show(cases.remember.register(screen.value.draft, id, kkmId, name))

    override fun edit(form: KkmForm) = screen.update { now ->
        val clean = form.copy(
            systemId = form.systemId.filter(Char::isDigit),
            token = form.token.filter(Char::isDigit),
            adminPin = Pin.digitsOf(form.adminPin),
            adminPinRepeat = Pin.digitsOf(form.adminPinRepeat),
            busy = now.form.busy
        )
        now.withForm(clean)
    }

    /**
     * Заводит кассу и, если она читается, объявляет её подключённой.
     *
     * Токен кабинета запрашивается в миг нажатия и нигде не задерживается:
     * ни на экране, ни в пройденном. Кабинет, ответивший без токена, —
     * не помеха кабинета, о которой скажет он сам: об этом говорится здесь,
     * иначе нажатие проходило молча.
     */
    override fun connect(onDone: () -> Unit) {
        val now = screen.value.takeIf { it.ready } ?: return
        screen.update { it.withForm(it.form.copy(busy = true)) }
        viewModelScope.launch {
            var notIssued = false
            val outcome = cases.enroll(now.plan) { tokenFor(now) { notIssued = true } }
            screen.update { it.withForm(it.form.copy(busy = false)) }
            if (notIssued) talk.say(INIT_KKM, Message.Refusal(textsOf(talk.language()).setup.noToken, NO_TOKEN))
            if (!outcome.announced(talk)) return@launch
            // Через кабинет пройденное забывается: касса подключена. Вручную —
            // стирается набранный токен: на экране ему не место.
            if (now.way == SetupWay.ByHand) {
                screen.update { it.copy(byHand = KkmForm()) }
            } else {
                show(cases.remember(KkmSetupDraft()))
            }
            reload()
            onDone()
        }
    }

    /**
     * Токен заводимой кассы: вручную — набранный, через кабинет — выпущенный сейчас.
     *
     * @param notIssued кабинет ответил, а токена не дал.
     */
    private suspend fun tokenFor(now: SetupUiState, notIssued: () -> Unit): String? {
        val issue = cases.issueToken
        val id = now.draft.cabinetRegisterId
        return when {
            now.way == SetupWay.ByHand -> now.form.token
            // Без кабинета или без кассы в нём выпускать токен некому и не для кого.
            issue == null || id == null -> null
            else -> calls.run("issue token") { issue(id).also { if (it == null) notIssued() } }
        }
    }

    private fun show(draft: KkmSetupDraft) = screen.update { it.copy(draft = draft, startingOver = false) }
}

/** Итог заведения — в строку сообщений; `true` — касса заведена и читается. */
private fun EnrollOutcome.announced(talk: Talk): Boolean {
    val texts = textsOf(talk.language()).setup
    when (this) {
        is EnrollOutcome.Enrolled -> talk.done(texts.connected)
        EnrollOutcome.NotStored -> talk.say(INIT_KKM, Message.Refusal(texts.notStored, NOT_STORED))
        is EnrollOutcome.Refused -> answer.shown(texts.stepAdmin, INIT_KKM, talk)
        EnrollOutcome.Skipped -> Unit
    }
    return this is EnrollOutcome.Enrolled
}

/** Обращение заведения кассы — для журнала. */
private const val INIT_KKM = "init kkm"

/** Код для строки сообщений: кабинет не выдал токен, и заводить кассу нечем. */
private const val NO_TOKEN = "KKM_TOKEN_NOT_ISSUED"

/** Код для строки сообщений: касса ответила на заведение, а сохранить её не смогла. */
private const val NOT_STORED = "KKM_NOT_STORED"
