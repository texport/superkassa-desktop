package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules

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
internal fun SignInSlot(session: Session, state: LoginState) {
    if (!state.atPin(session)) return
    val scope = rememberCoroutineScope()
    val chosen = state.chosenKkm(session)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.width(Sizes.loginColumn).padding(Spacing.roomy)) {
            SignInBar(
                pin = state.pin,
                nameOf = { session.displayName(it) },
                onPin = { state.pin = UserRules.digitsOf(it) },
                chosen = chosen,
                // Перечитывание состояния делает каркас — он живёт всё
                // время работы, а экран входа исчезает в тот же миг,
                // и запущенное там обновление обрывалось бы на полпути.
                onEnter = { chosen?.let { kkm -> scope.launch { session.signIn(kkm, state.pin) } } },
                onReload = { scope.launch { session.refreshKkms() } }
            )
        }
    }
}
