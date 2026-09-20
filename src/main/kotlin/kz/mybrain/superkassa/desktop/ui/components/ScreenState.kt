package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Что стоит на месте содержимого.
 *
 * Четыре случая, и решаются они в одном месте на всё приложение: ответа
 * ещё нет, ответ пуст, ответ — отказ, ответ есть. Прежде каждый экран
 * разбирал их сам и по-своему: журнал показывал кружок, аналитика —
 * свой кружок с подписью, кабинет — пустоту без единого слова.
 */
sealed interface ScreenState {

    /** Ответа ещё нет. */
    data object Working : ScreenState

    /** Ответ пришёл и он пуст: сказать, что здесь бывает и что сделать. */
    data class Empty(
        val icon: ImageVector,
        val title: String,
        val hint: String? = null
    ) : ScreenState

    /**
     * Ответа не будет: отказ службы или обрыв связи.
     *
     * Слова берутся у того, кто отказал, — свои формулировки поверх чужого
     * отказа скрывают причину. Повтор предлагается там, где повторять
     * есть чем.
     */
    data class Trouble(
        val title: String,
        val hint: String? = null,
        val onRetry: (() -> Unit)? = null
    ) : ScreenState

    /** Содержимое на месте. */
    data object Ready : ScreenState
}

/**
 * Место под содержимое экрана вместе с его ожиданием, пустотой и отказом.
 *
 * Экран объявляет состояние и отдаёт его сюда, а не заводит свой кружок,
 * свою надпись «читается» и свою строку отказа.
 *
 * @param dense плотный вид для области внутри карточки или списка.
 * @param centered прижать объяснение к середине области, а не к её верху.
 */
@Composable
fun ScreenSlot(
    state: ScreenState,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    centered: Boolean = false,
    content: @Composable () -> Unit
) {
    if (filled(state, modifier, dense, centered)) return
    content()
}

/**
 * То же в колонке экрана.
 *
 * Отдельная запись нужна только ради `Modifier.weight` и строк, которые
 * содержимое кладёт прямо в колонку: решение остаётся одно и то же
 * и принимается в [filled].
 */
@Composable
fun ColumnScope.ScreenSlot(
    state: ScreenState,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (filled(state, modifier, dense, centered)) return
    content()
}

/**
 * Единственное место, где решается, что стоит вместо содержимого.
 *
 * @return `true` — место занято ожиданием, пустотой или отказом.
 */
@Composable
private fun filled(state: ScreenState, modifier: Modifier, dense: Boolean, centered: Boolean): Boolean {
    when (state) {
        ScreenState.Working -> LoadingState(modifier, dense)
        is ScreenState.Empty -> EmptyState(state.icon, state.title, state.hint, modifier, dense, centered)
        is ScreenState.Trouble -> TroubleState(state, modifier, dense, centered)
        ScreenState.Ready -> return false
    }
    return true
}

/**
 * Отказ словами отказавшего и кнопка повтора.
 *
 * Повторить предлагается и тогда, когда службы ещё нет вовсе: выкладка
 * проходит без участия владельца, и после неё экран обязан заработать
 * по нажатию, а не по перезапуску приложения.
 */
@Composable
private fun TroubleState(
    trouble: ScreenState.Trouble,
    modifier: Modifier,
    dense: Boolean,
    centered: Boolean
) {
    val retry = trouble.onRetry
    if (retry == null) {
        EmptyState(AppIcons.warning, trouble.title, trouble.hint, modifier, dense, centered)
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug, Alignment.CenterVertically)
    ) {
        EmptyState(AppIcons.warning, trouble.title, trouble.hint, dense = dense)
        FilledTonalButton(onClick = retry) { Text(LocalStrings.current.common.retry) }
    }
}
