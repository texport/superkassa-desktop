package kz.mybrain.superkassa.presentation.cabinet.signin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * То же действие для мастера подключения: вход — в кабинет окна, но без
 * чтения хозяйства сети — мастеру нужна одна касса (см. `signInForOne`).
 */
@Composable
internal fun SignInAction(
    cabinet: CabinetViewModel,
    language: Language,
    texts: CabinetTexts,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val state by cabinet.state.collectAsScreenState()
    SignInAction(state, language, texts, cabinet.actionsForOne(), modifier, cabinet.signature)
}
