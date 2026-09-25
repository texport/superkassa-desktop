package kz.mybrain.superkassa.presentation.settings.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.settings.model.secondsOf
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Настройки кассы на этой машине: сроки обмена с БФД и сведения о ней.
 *
 * Одни на все кассы рабочего места и пина не спрашивают. Правку решает
 * касса: в режиме сервера и под запретом владельца она отказывает, и это
 * видно на карточке до нажатия. Отказ, пришедший всё же, называется
 * своими словами — см. [savedCore].
 */
class CoreSettingsViewModel(
    private val cases: CoreSettingsCases,
    private val talk: Talk
) : ViewModel(), CoreSettingsActions {
    private val screen = MutableStateFlow(CoreSettingsUiState())
    private val busy = Busy()

    val state: StateFlow<CoreSettingsUiState> = screen.asStateFlow()

    init {
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
        reload()
    }

    override fun reload() {
        viewModelScope.launch {
            val facts = cases.facts()
            screen.update { it.copy(about = facts) }
        }
        viewModelScope.launch {
            val texts = textsOf(talk.language()).settings.core
            val settings = cases.read().shown(texts.title, "read core settings", talk)
            if (settings != null) screen.update { it.copy(settings = settings) }
        }
    }

    override fun typeTimeout(text: String) = screen.update { it.copy(timeoutDraft = text) }

    override fun typeReconnect(text: String) = screen.update { it.copy(reconnectDraft = text) }

    /** Набранное забывается только после согласия кассы. */
    override fun save() {
        val now = screen.value
        val timeout = secondsOf(now.timeout)
        val reconnect = secondsOf(now.reconnect)
        if (now.settings == null || timeout == null || reconnect == null) return
        whileBusy(busy) {
            val texts = textsOf(talk.language()).settings.core
            val saved = talk.savedCore(cases.save(timeout, reconnect), texts.title, "save core settings", now.server)
            saved ?: return@whileBusy
            screen.update { CoreSettingsUiState(settings = saved, busy = it.busy) }
            talk.done(texts.saved)
        }
    }
}
