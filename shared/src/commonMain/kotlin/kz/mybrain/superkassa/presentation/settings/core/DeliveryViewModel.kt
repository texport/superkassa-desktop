package kz.mybrain.superkassa.presentation.settings.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.DeliveryRules
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.strings.settings.deliveryTexts

/**
 * Каналы доставки чека: SMS, Telegram, WhatsApp и почта — включены ли,
 * кому шлют и через какие службы.
 *
 * Одни на все кассы рабочего места и пина не спрашивают. Ключи каналов
 * в модель не попадают — в полях стоит их знак. Правку решает касса:
 * закрытую видно на карточке до нажатия, а отказ, пришедший всё же,
 * называется словами владельца — см. [savedCore].
 */
class DeliveryViewModel(private val cases: DeliveryCases, private val talk: Talk) : ViewModel(), DeliveryActions {
    private val screen = MutableStateFlow(DeliveryUiState())
    private val busy = Busy()

    val state: StateFlow<DeliveryUiState> = screen.asStateFlow()

    init {
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
        viewModelScope.launch {
            val texts = deliveryTexts(talk.language())
            val settings = cases.read().shown(texts.title, "read delivery settings", talk) ?: return@launch
            screen.update { DeliveryUiState.of(settings, it.busy) }
        }
    }

    /** Ключ набирается поверх знака заданного: см. [DeliveryRules.typedSecret]. */
    override fun type(field: DeliveryField, text: String) = screen.update { now ->
        val typed = if (field.secret) DeliveryRules.typedSecret(now.value(field), text) else text
        now.copy(drafts = now.drafts + (field to typed))
    }

    override fun switch(channel: DeliveryChannel, on: Boolean) =
        screen.update { it.copy(switched = it.switched + (channel to on)) }

    /** Набранное забывается только после согласия кассы. */
    override fun save() {
        val now = screen.value
        if (!now.savable) return
        whileBusy(busy) {
            val texts = deliveryTexts(talk.language())
            val answer = cases.save(now.drafts, now.switched)
            val saved = talk.savedCore(answer, texts.title, "save delivery settings", now.server) ?: return@whileBusy
            screen.update { DeliveryUiState.of(saved, it.busy) }
            talk.done(texts.saved)
        }
    }
}
