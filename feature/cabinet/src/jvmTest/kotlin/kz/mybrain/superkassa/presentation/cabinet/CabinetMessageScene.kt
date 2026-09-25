package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.presentation.common.message.MessageEffect
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Помеха кабинета так, как её видит владелец: всплывающей строкой окна.
 *
 * Отказ входа, недоступный кабинет и неотвеченный NCALayer показываются
 * не на месте содержимого, а снекбаром каркаса. Снимать их поверх экрана
 * входа — единственный способ увидеть то, что видит владелец.
 */
@Composable
internal fun WithCabinetMessage(problem: CabinetProblem, content: @Composable () -> Unit) {
    val messages = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { MessageHost(messages) }) {
        MessageEffect(cabinetMessage(problem, textsOf(Language.Ru).cabinet), messages) {}
        content()
    }
}
