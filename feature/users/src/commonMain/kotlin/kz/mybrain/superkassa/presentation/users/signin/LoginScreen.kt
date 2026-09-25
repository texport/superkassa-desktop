package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.field.SearchField
import kz.mybrain.superkassa.designsystem.state.LoadingState
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Вход в кассу.
 *
 * Кассир выбирает свою кассу и вводит пин один раз за смену — дальше пин
 * не спрашивается ни на одном экране. Касса запоминается: на рабочем месте
 * она не меняется, и утром достаточно ввести пин.
 *
 * Блоки идут сверху вниз без разрывов: поиск, перечень, двери и полоса
 * пина — с шагом общей шкалы. Заголовок «Вход в кассу», тема и язык стоят
 * в шапке окна: её ставит каркас, одну на вход и на всё, что открыто
 * дверями отсюда. Перечень берёт высоту своих строк
 * и прокручивается, только когда касс больше, чем помещается; полоса пина
 * встаёт сразу под дверями. Прежде она была прибита к низу окна, а
 * перечень растягивался до неё пустой рамкой, и между дверями и пином
 * стояла пустота в пол-экрана.
 *
 * Снизу колонка держит запас под снекбар ([Sizes.snackbarRoom]): отказ
 * входа встаёт у нижнего края по центру и не закрывает поле пина, даже
 * когда перечень занял всю высоту.
 *
 * @param doors двери, за которыми на этой платформе что-то есть.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    actions: LoginActions,
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
            modifier = Modifier
                .widthIn(max = Sizes.loginColumn)
                .fillMaxWidth()
                .padding(bottom = Sizes.snackbarRoom),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            when {
                state.kkms.isEmpty() && !state.answered -> LoadingState(Modifier.weight(1f, fill = false))
                state.kkms.isEmpty() -> EmptyKkms(state.listRead, actions, doors)
                else -> KkmChoice(state, actions, doors, Modifier.weight(1f, fill = false))
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
    // Удачный вход меняет держатель входа, и окно само уходит к работе:
    // экрану входа об этом знать незачем.
    SignInBar(state, actions)
}
