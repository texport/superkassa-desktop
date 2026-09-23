package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDoor
import kz.mybrain.superkassa.desktop.ui.components.LoadingState
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
 */
@Composable
fun LoginScreen(session: Session, cabinet: CabinetSession, state: LoginState) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        session.refreshKkms()
        state.answered = true
    }

    val reload = { scope.launch { session.refreshKkms() } }
    val shut = { state.door = Door.Kkms }
    when (state.door) {
        // Заведение кассы открывается прямо отсюда: пока не заведена
        // первая касса, войти некуда, а настройки живут за входом.
        Door.Register -> return ConnectKkmScreen(session, cabinet, shut)
        // Кабинет открывается до выбора кассы и без пина: пока первая
        // касса не заведена, пина кассира не существует вовсе, а завести
        // кассу можно только начав с кабинета.
        Door.Cabinet -> return CabinetDoor(session, cabinet, shut)
        // Настройки рабочего места открываются до входа: адрес узла
        // и адрес кабинета нужны раньше, чем есть куда войти.
        Door.Settings -> return WorkplaceSettingsScreen(session, shut)
        Door.Kkms -> Unit
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Поля и шаг между блоками задаёт колонка, а не каждый блок сам:
        // когда шапка, список и двери несли по своему отступу, все
        // зазоры выходили разными.
        Column(
            modifier = Modifier.width(Sizes.loginColumn).fillMaxSize().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            LoginHeader(session)
            if (session.kkms.isEmpty() && !state.answered) {
                LoadingState(Modifier.weight(1f))
                return@Column
            }
            if (session.kkms.isEmpty()) {
                EmptyKkms(
                    // Прочитанный пустой список и непрочитанный — разные
                    // беды: у первого касс правда нет, о втором неизвестно
                    // ничего. Отказ узла — тоже ответ, но не о кассах.
                    listRead = session.kkmsRead,
                    onReload = { reload() },
                    onCabinet = { state.door = Door.Cabinet },
                    onRegister = { state.door = Door.Register },
                    onSettings = { state.door = Door.Settings }
                )
                return@Column
            }
            // Набранный номер отменяет прежний выбор мышью: он сделан позже,
            // а значит и означает намерение кассира. Иначе набор «2000042»
            // после клика по другой кассе не менял ничего, и кассир входил
            // не туда, куда набрал.
            SearchField(
                value = state.search,
                label = texts.login.search,
                icon = AppIcons.kkm,
                onChange = {
                    state.search = it
                    state.chosen = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            KkmList(
                kkms = state.shown(session),
                nameOf = { session.displayName(it) },
                chosenId = state.chosenKkm(session)?.kkmId,
                rememberedId = session.rememberedKkmId,
                modifier = Modifier.weight(1f)
            ) { state.chosen = it }
            // Две двери рядом: кассир входит пином ниже, владелец —
            // своей ЭЦП в кабинет. Обе со значками и в рамке: текстовыми
            // вподбор они терялись, а кабинет для нового владельца —
            // единственный вход, пока нет ни кассы, ни компании.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                DoorButton(AppIcons.newKkm, texts.sections.register) { state.door = Door.Register }
                DoorButton(AppIcons.cabinet, texts.sections.cabinet) { state.door = Door.Cabinet }
                DoorButton(AppIcons.settings, texts.sections.settings) { state.door = Door.Settings }
            }
        }
    }
}
