package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

/**
 * Рельс разделов: пункты рельса Material 3 столбцом, а над ними — ряд
 * ростом с шапку окна, и в нём кнопка меню против заголовка.
 *
 * Свой столбец, а не `WideNavigationRail`: тот ставит шапку с кнопкой меню
 * на 44 точки ниже верха окна и отделяет её от пунктов ещё 40 — кнопка
 * стояла ниже заголовка окна, а между ней и первым разделом была пустота
 * в два пункта. Пункты — те же `NavigationSuiteItem` Material 3 со своими
 * цветами, отметкой и подписями; своих компонентов здесь нет.
 *
 * Разделы прокручиваются: у администратора их десять, а окно кассы бывает
 * ростом в 640 точек — на ноутбуке и на экране прилавка.
 *
 * @param onMenu открыть все разделы поверх окна; `null` — кнопки нет,
 *   ряд шапки остаётся пустым, и разделы стоят на той же высоте.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SectionRail(onMenu: (() -> Unit)?, items: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(Sizes.rail)
            .windowInsetsPadding(RailInsets),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuRow {
            onMenu?.let { open ->
                IconButton(onClick = open) {
                    Icon(AppIcons.menu, contentDescription = LocalStrings.current.general.expand)
                }
            }
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { items() }
    }
}

/**
 * Ряд кнопки меню у рельса: ростом со строку шапки окна и шириной рельса,
 * кнопка — посередине, на оси значков разделов. Тот же ряд стоит сверху
 * у панели разделов поверх окна: кнопка закрытия встаёт на место кнопки
 * меню, а не прыгает в другой угол.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MenuRow(button: @Composable () -> Unit) {
    Box(
        modifier = Modifier.height(TopAppBarDefaults.TopAppBarExpandedHeight).width(Sizes.rail),
        contentAlignment = Alignment.Center
    ) { button() }
}

/** Края окна, от которых отступают рельс и панель разделов: верх, низ и начало. */
internal val RailInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
