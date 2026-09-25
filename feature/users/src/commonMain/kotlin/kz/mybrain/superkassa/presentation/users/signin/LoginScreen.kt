package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
 * Это первый раздел окна до входа — «Кассы». Заведение кассы, кабинет
 * БФД и настройки — соседние разделы той же навигации окна, а не кнопки
 * под списком: прежде они стояли здесь дверями, за которыми открывалась
 * другая страница со своей стрелкой назад.
 *
 * Блоки идут сверху вниз без разрывов: поиск, перечень и полоса пина —
 * с шагом общей шкалы. Заголовок «Вход в кассу», тема и язык стоят
 * в шапке окна. Перечень берёт высоту своих строк и прокручивается,
 * только когда касс больше, чем помещается; полоса пина встаёт сразу
 * под ним. Прежде она была прибита к низу окна, а перечень растягивался
 * до неё пустой рамкой, и между списком и пином стояла пустота
 * в пол-экрана.
 *
 * Снизу колонка держит запас под снекбар ([Sizes.snackbarRoom]): отказ
 * входа встаёт у нижнего края по центру и не закрывает поле пина, даже
 * когда перечень занял всю высоту.
 *
 * @param onRegister открыть раздел «Новая касса» — главное действие, когда
 *   касс нет; `null` — мастера на этой платформе нет.
 */
@Composable
fun LoginScreen(state: LoginUiState, actions: LoginActions, onRegister: (() -> Unit)? = null) {
    // Список перечитывается при каждом возврате в раздел: в соседнем
    // «Новая касса» кассу могли только что завести.
    LaunchedEffect(Unit) { actions.reload() }
    // Поля и шаг между блоками задаёт колонка, а не каждый блок сам:
    // когда шапка, список и двери несли по своему отступу, все зазоры
    // выходили разными. Колонка — во всю ширину раздела, по тем же краям,
    // что соседние «Новая касса» и «Настройки»: узкая колонка посередине
    // прыгала краями при переходе между разделами.
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = Sizes.snackbarRoom),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        when {
            state.kkms.isEmpty() && !state.answered -> LoadingState(Modifier.weight(1f, fill = false))
            state.kkms.isEmpty() -> EmptyKkms(state.listRead, actions, onRegister)
            else -> KkmChoice(state, actions, Modifier.weight(1f, fill = false))
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
private fun KkmChoice(state: LoginUiState, actions: LoginActions, modifier: Modifier) {
    val texts = LocalStrings.current
    SearchField(
        value = state.search,
        label = texts.login.search,
        icon = AppIcons.kkm,
        onChange = actions::search,
        modifier = Modifier.fillMaxWidth()
    )
    KkmList(state, modifier, onPick = actions::pick)
    // Удачный вход меняет держатель входа, и окно само уходит к работе:
    // экрану входа об этом знать незачем.
    SignInBar(state, actions)
}
