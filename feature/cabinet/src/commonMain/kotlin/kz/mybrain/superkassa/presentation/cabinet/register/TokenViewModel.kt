package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.shown

/** Выданный токен и чьей кассе: показывается один раз и только у неё. */
internal data class IssuedToken(val registerId: String, val token: Long)

/**
 * Технический токен кассы.
 *
 * Выдаётся по требованию владельца и показывается один раз: это ключ,
 * которым касса подписывает запросы, и место ему в настройках кассы.
 *
 * Кассе, которая работает на этой же машине, новый токен вписывается
 * сразу — в кассу процесса: БФД отзывает прежний, и касса, о новом
 * не знающая, на первом же чеке получила бы «неверный токен» и блокировку.
 * Вписать можно только в ту кассу, в которую вошли: пин принадлежит ей.
 */
internal class TokenViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val shown = MutableStateFlow<IssuedToken?>(null)

    val state: StateFlow<IssuedToken?> = shown.asStateFlow()

    /** @param here касса этой машины, заведённая под этой кассой кабинета. */
    fun issue(registerId: String, here: KkmResponse?) {
        val cases = cabinet.useCases
        viewModelScope.launch {
            val issued = cabinet.work.run("issue token") { cases.issueToken(registerId) }.value ?: return@launch
            val texts = cabinet.texts
            // Неподтверждённого токена нет: ни показать, ни вписать в кассу —
            // касса с ним получила бы «неверный токен» и блокировку.
            val token = issued.token ?: return@launch cabinet.talk.say(
                "issue token",
                Message.Refusal(texts.register.tokenUnconfirmed, TOKEN_UNCONFIRMED)
            )
            shown.value = IssuedToken(registerId, token)
            val written = cases.writeToken(here, token.toString()) ?: return@launch
            written.shown(texts.register.issueToken, "write issued token", cabinet.talk) ?: return@launch
            cabinet.talk.done(texts.register.tokenGoesToNode)
        }
    }
}

/** Код, которым приложение называет токен, не подтверждённый сервисом приёма. */
private const val TOKEN_UNCONFIRMED = "TOKEN_PENDING"

/** Модель выдачи токена окна. */
@Composable
internal fun tokenViewModel(cabinet: CabinetViewModel): TokenViewModel = viewModel { TokenViewModel(cabinet) }
