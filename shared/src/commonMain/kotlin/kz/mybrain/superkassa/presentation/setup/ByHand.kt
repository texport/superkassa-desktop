package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.CardSequence
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.setup.component.FactoryStepCard
import kz.mybrain.superkassa.presentation.setup.component.OfdStep
import kz.mybrain.superkassa.presentation.setup.component.SetupStepCard
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Ручной путь: заводской номер и касса по идентификатору и токену.
 *
 * Заводской номер тот же и запоминается так же: свой шаг для ручного пути
 * выдал бы владельцу второй номер, отличный от унесённого в кабинет.
 * Второй шаг доступен сразу: кассу в БФД завели без владельца, и номер
 * отсюда ему не нужен — идентификатор и токен уже на руках.
 */
@Composable
fun ByHand(state: SetupUiState, actions: SetupActions) {
    val setup = textsOf(LocalLanguage.current).setup
    Text(
        text = setup.manuallyHint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    CardSequence(Modifier.fillMaxWidth()) {
        FactoryStepCard(state, actions, setup)
        SetupStepCard(title = setup.stepAdmin, hint = setup.stepAdminHint, texts = setup, done = false, ready = true) {
            OfdStep(state, actions)
        }
    }
}

/**
 * Мастер без кабинета: только ручной путь.
 *
 * Там, где кабинета нет — на Android нет подписи ЭЦП, — выбирать путь
 * не из чего, и переключатель путей не ставится: кассу, заведённую в БФД
 * сервисником, владелец подключает идентификатором и токеном.
 *
 * @param onBack возврат туда, откуда пришли; `null` — возвращаться некуда.
 */
@Composable
fun ConnectByHand(model: SetupViewModel, onBack: (() -> Unit)?) {
    val state by model.state.collectAsScreenState()
    // Контуры и кассы читаются, как мастер открыт: справочник мог не ответить прежде.
    LaunchedEffect(Unit) { model.reload() }
    SetupFrame(state, model, onBack) { ByHand(state, model) }
}
