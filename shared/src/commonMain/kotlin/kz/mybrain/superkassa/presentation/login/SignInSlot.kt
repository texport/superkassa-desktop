package kz.mybrain.superkassa.presentation.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Пин и вход в нижнем слоте окна.
 *
 * Полоса стоит слотом каркаса, а не последним блоком экрана: Material 3
 * кладёт снекбар над нижней полосой, и отказ входа больше не закрывает
 * ровно то, что кассир должен исправить, — поле пина и кнопку «Войти».
 * В окне 1000×700 на крупной ступени шрифта сообщение об отказе ложилось
 * на нижнюю треть полосы.
 *
 * Полоса держится тех же полей, что и список над ней: разъехавшиеся
 * по ширине блоки на одном экране читаются как разные разделы.
 */
@Composable
fun SignInSlot(state: LoginUiState, actions: LoginActions) {
    if (!state.atPin) return
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = Sizes.loginColumn).padding(Spacing.roomy)) {
            // Удачный вход меняет держатель входа, и окно само уходит
            // к работе: экрану входа об этом знать незачем.
            SignInBar(state, actions)
        }
    }
}
