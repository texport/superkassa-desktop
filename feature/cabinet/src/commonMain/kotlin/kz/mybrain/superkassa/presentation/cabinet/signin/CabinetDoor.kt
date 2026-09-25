package kz.mybrain.superkassa.presentation.cabinet.signin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.common.navigation.LocalToKassa

/**
 * Кабинет ОФД, открытый с экрана входа.
 *
 * Владелец приходит в кабинет до всякой кассы: пока она не заведена,
 * пина кассира не существует, а завести её без кабинета нельзя. Поэтому
 * дверь в кабинет стоит рядом с входом кассира, а не за ней.
 *
 * Экран тот же, что и в разделе после входа. Шапку кабинета и полоску
 * ожидания ставит каркас окна в свой единственный слот шапки — так же,
 * как для раздела кабинета после входа: у двери своей шапки нет, и шапка
 * окна не прыгает, когда владелец открывает дверь. Окно просмотра
 * печатной формы стоит над обоими входами и живёт в каркасе окна.
 *
 * @param onBack возврат на вход: им же «перейти к кассе» уводит из двери.
 * @param stepped открыта карточка точки или кассы шагом истории окна.
 */
@Composable
fun CabinetDoor(window: CabinetWindow, onBack: () -> Unit, stepped: Boolean = false) {
    // За дверью разделов кассы нет, и просьба показать раздел значит одно:
    // выйти из кабинета на вход, где уже выбрана нужная касса.
    CompositionLocalProvider(LocalToKassa provides onBack) {
        Column(modifier = Modifier.fillMaxSize()) { CabinetScreen(window, stepped) }
    }
}
