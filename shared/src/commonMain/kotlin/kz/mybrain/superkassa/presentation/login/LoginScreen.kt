package kz.mybrain.superkassa.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.field.SearchField
import kz.mybrain.superkassa.presentation.common.state.LoadingState
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Вход в кассу.
 *
 * Кассир выбирает свою кассу и вводит пин один раз за смену — дальше пин
 * не спрашивается ни на одном экране. Касса запоминается: на рабочем месте
 * она не меняется, и утром достаточно ввести пин.
 *
 * Раскладка та же, что у списка с действием по Material 3: перечень
 * прокручивается, а пин и кнопка стоят в нижней полосе окна и никуда
 * не уезжают, сколько бы касс ни было в списке. Саму полосу рисует
 * каркас — [SignInSlot]: так снекбар отказа встаёт над ней, а не поверх
 * поля пина.
 *
 * @param doors двери, за которыми на этой платформе что-то есть.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    actions: LoginActions,
    header: @Composable RowScope.() -> Unit,
    doors: Set<Door>,
    door: @Composable (Door, close: () -> Unit) -> Unit
) {
    LaunchedEffect(Unit) { actions.reload() }
    // Заведение кассы, кабинет и настройки открываются прямо отсюда: пока
    // не заведена первая касса, войти некуда, а адрес кабинета нужен раньше,
    // чем есть куда войти. Что за дверью — знает каркас окна, не вход.
    if (state.door != Door.Kkms) return door(state.door) { actions.open(Door.Kkms) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Поля и шаг между блоками задаёт колонка, а не каждый блок сам:
        // когда шапка, список и двери несли по своему отступу, все
        // зазоры выходили разными.
        Column(
            modifier = Modifier.widthIn(max = Sizes.loginColumn).fillMaxSize().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            LoginHeader(header)
            when {
                state.kkms.isEmpty() && !state.answered -> LoadingState(Modifier.weight(1f))
                state.kkms.isEmpty() -> EmptyKkms(state.listRead, actions, doors)
                else -> KkmChoice(state, actions, doors, Modifier.weight(1f))
            }
        }
    }
}

/**
 * Поиск, перечень касс и двери под ним.
 *
 * Набранный номер отменяет прежний выбор мышью: он сделан позже, а значит
 * и означает намерение кассира. Иначе набор «2000042» после клика по другой
 * кассе не менял ничего, и кассир входил не туда, куда набрал.
 */
@Composable
private fun KkmChoice(state: LoginUiState, actions: LoginActions, doors: Set<Door>, modifier: Modifier) {
    val texts = LocalStrings.current
    SearchField(
        value = state.search,
        label = texts.login.search,
        icon = AppIcons.kkm,
        onChange = actions::search,
        modifier = Modifier.fillMaxWidth()
    )
    KkmList(state, modifier, onPick = actions::pick)
    // Две двери рядом: кассир входит пином ниже, владелец — своей ЭЦП
    // в кабинет. Обе со значками и в рамке: кабинет для нового владельца —
    // единственный вход, пока нет ни кассы, ни компании.
    WrapRow { DoorButtons(doors, listOf(Door.Register, Door.Cabinet, Door.Settings), actions) }
}
