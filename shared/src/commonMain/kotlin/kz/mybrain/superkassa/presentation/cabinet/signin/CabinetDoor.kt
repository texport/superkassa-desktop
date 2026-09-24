package kz.mybrain.superkassa.presentation.cabinet.signin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.presentation.cabinet.CabinetBar
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.navigation.LocalToKassa

/**
 * Кабинет ОФД, открытый с экрана входа.
 *
 * Владелец приходит в кабинет до всякой кассы: пока она не заведена,
 * пина кассира не существует, а завести её без кабинета нельзя. Поэтому
 * дверь в кабинет стоит рядом с входом кассира, а не за ней.
 *
 * Экран тот же, что и в разделе после входа, и шапка та же. Возврата
 * своего у двери нет: стрелку рисует шапка и она же решает, куда вести —
 * к карточке кассы, пока открыты её документы, или обратно на вход.
 *
 * Полоска ожидания под шапкой такая же, как у кассы: владелец ждёт ответа
 * кабинета так же, как кассир — ответа кассы, и прежде у двери её не было
 * вовсе. Окно просмотра печатной формы стоит над обоими входами и живёт
 * в каркасе окна — второго здесь не заводится.
 */
@Composable
fun CabinetDoor(window: CabinetWindow, onBack: () -> Unit) {
    val state by window.cabinet.state.collectAsScreenState()
    // За дверью разделов кассы нет, и просьба показать раздел значит одно:
    // выйти из кабинета на вход, где уже выбрана нужная касса. Прежде
    // просьба уходила в пустоту, и «Перейти к кассе» с виду не делала
    // ничего — ровно после переноса кассы на эту машину, когда владелец
    // приходит в кабинет именно отсюда.
    CompositionLocalProvider(LocalToKassa provides onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            CabinetBar(window.cabinet, window.look, onExit = onBack)
            BusyLine(state.busy)
            CabinetScreen(window)
        }
    }
}
