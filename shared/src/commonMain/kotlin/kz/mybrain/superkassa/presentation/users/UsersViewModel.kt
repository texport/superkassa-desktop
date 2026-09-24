package kz.mybrain.superkassa.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.Pin
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.presentation.common.message.words
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Кассиры кассы: список, заведение, смена пина, удаление.
 *
 * Пин меняют и себе, и другим, и последствия у этого разные: сменивший
 * пин себе продолжает работу новым пином, а сменивший его кассиру —
 * своим прежним. Новый пин принадлежит тому, кому его задали, и чеки
 * администратора под ним не подписываются никогда. Удаливший себя
 * выходит из кассы: касса вместе с кассиром забыла и его пин.
 */
class UsersViewModel(private val cases: UsersCases, private val talk: Talk) : ViewModel(), UsersActions {
    private val screen = MutableStateFlow(UsersUiState())
    private val texts: AppStrings get() = textsOf(talk.language()).common

    val state: StateFlow<UsersUiState> = screen.asStateFlow()

    init {
        followSeat(
            cases.observe(),
            each = { signIn -> screen.update { it.copy(me = signIn.cashier, kkmChosen = signIn.kkm != null) } },
            seated = ::reseat
        )
        viewModelScope.launch { cases.readRoleNames()?.let { names -> screen.update { it.copy(roleNames = names) } } }
    }

    override fun reload() {
        viewModelScope.launch { read() }
    }

    override fun edit(form: CashierForm) = screen.update {
        it.copy(form = form.copy(pin = Pin.digitsOf(form.pin), busy = it.form.busy))
    }

    /** Заводит кассира; итог объявляется после перечитанного списка — оно снимает прошлое сообщение. */
    override fun create() {
        val form = screen.value.takeIf { it.canCreate }?.form ?: return
        screen.update { it.copy(form = form.copy(busy = true)) }
        viewModelScope.launch {
            val answer = cases.createCashier(form.name, form.role, form.pin)
            val created = answer?.shown(texts.users.create, "create user", talk)
            screen.update { it.copy(form = if (created == null) form else CashierForm()) }
            created ?: return@launch
            read()
            val role = roleWord(created.role, texts.users, screen.value.roleNames, talk.language())
            talk.done("$role ${texts.users.created}: ${created.name}")
        }
    }

    override fun askPin(user: UserResponse?) = screen.update { now ->
        now.copy(pin = user?.let { PinChange(it, own = now.own(it)) })
    }

    override fun typeNewPin(text: String) = screen.update { now ->
        now.copy(pin = now.pin?.copy(pin = UserRules.digitsOf(text), refusal = null))
    }

    /**
     * Задаёт кассиру набранный пин.
     *
     * Чей пин станет пином работающего, решает сценарий смены пина: только
     * свой. Список перечитывается пином работающего — уже новым, если
     * сменён свой.
     */
    override fun confirmPin() {
        val change = screen.value.pin?.takeIf { it.ready } ?: return
        screen.update { it.copy(pin = change.copy(busy = true)) }
        viewModelScope.launch {
            val answer = cases.changeCashierPin(change.user, change.pin) ?: return@launch
            if (answer !is Answer.Done) return@launch screen.update { it.copy(pin = change.refused(answer, talk)) }
            screen.update { it.copy(pin = null) }
            read()
            talk.done("${change.user.name} — ${texts.users.changed}")
        }
    }

    override fun askRemove(user: UserResponse?) = screen.update { it.copy(removing = user) }

    /** Удаляет кассира, о котором спросили; удаливший себя выходит из кассы. */
    override fun confirmRemove() {
        val user = screen.value.removing ?: return
        screen.update { it.copy(removing = null) }
        viewModelScope.launch {
            val self = cases.removeCashier(user)?.shown(texts.users.delete, "delete user", talk) ?: return@launch
            if (!self) read()
            talk.done("${user.name} — ${texts.users.deleted}")
        }
    }

    /** За кассой сел другой: прочитанный список принадлежал прежнему, набранное остаётся. */
    private fun reseat(signIn: SignInState) {
        screen.update { UsersUiState(kkmChosen = it.kkmChosen, me = it.me, roleNames = it.roleNames, form = it.form) }
        // Модель окна переживает смену кассира: кассиру список касса не отдаёт,
        // и спрашивать её за него значило показать ему отказ ни за что.
        if (signIn.signedIn && signIn.isAdmin) reload()
    }

    /**
     * Перечитывает список; отказ кассы запоминается, иначе экран вечно ждал бы ответа.
     * За кассой никого нет — спрашивать некого.
     */
    private suspend fun read() {
        if (!cases.observe().value.signedIn) return
        val list = cases.readCashiers().shown(texts.users.title, "read users", talk)
        screen.update {
            if (list == null) it.copy(unreadable = true) else it.copy(users = list, answered = true, unreadable = false)
        }
    }
}

/**
 * Отказ кассы остаётся в окне смены пина, её словами.
 *
 * Сбой — не отказ по существу: исправить его другим пином нельзя,
 * и о нём говорит строка сообщений, как везде.
 */
private fun PinChange.refused(answer: Answer<*>, talk: Talk): PinChange {
    val refusal = answer as? Answer.Refused
    if (refusal == null) answer.shown(textsOf(talk.language()).common.users.change, "change user pin", talk)
    refusal?.let { talk.journal.warn("change user pin: refused ${it.code}") }
    return copy(busy = false, refusal = refusal?.words(talk.language()))
}
