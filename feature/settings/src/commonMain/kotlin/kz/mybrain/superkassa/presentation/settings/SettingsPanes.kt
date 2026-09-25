package kz.mybrain.superkassa.presentation.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.adaptive.listDetailDirective
import kz.mybrain.superkassa.designsystem.keyboard.SystemBack

/**
 * Настройки «списком и подробностями» Material 3.
 *
 * Слева — список разделов, справа — настройки открытого раздела одной
 * колонкой. Так Material 3 велит раскладывать настройки на большом экране
 * (Canonical layouts → List-detail): прежние вкладки с карточками столбцами
 * вставали на мониторе вразнобой по высоте.
 *
 * Сколько панелей рядом, решает класс окна ([listDetailDirective]):
 * на телефоне, складном и планшете стоймя раздел открывается поверх
 * списка, и назад к списку ведут жест Android, Escape и стрелка в заголовке
 * раздела. На расширенном окне и шире обе панели стоят рядом, и открытый
 * раздел подсвечен в списке; пока ничего не выбрано, открыт первый.
 *
 * Раздел, которого вошедшему не видно — кассир сменил администратора, —
 * уступает место первому видимому, а не остаётся пустой панелью.
 *
 * @param titled у списка свой заголовок «Настройки»; `false` — его уже
 *   называет шапка окна настроек рабочего места.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsPanes(board: SettingsBoard, modifier: Modifier = Modifier, titled: Boolean = true) {
    val sections = board.sections
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsSection>(scaffoldDirective = listDetailDirective())
    val scope = rememberCoroutineScope()
    val open = navigator.currentDestination?.contentKey?.takeIf { it in sections } ?: sections.first()
    val beside = navigator.besideEachOther()
    val back: (() -> Unit)? = if (navigator.canNavigateBack()) ({ scope.launch { navigator.navigateBack() } }) else null
    SystemBack(enabled = back != null) { back?.invoke() }
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                SettingsList(board, sections, open.takeIf { beside }, titled) { chosen ->
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chosen) }
                }
            }
        },
        detailPane = { AnimatedPane { SectionPane(board, open, back.takeUnless { beside }) } },
        modifier = modifier
    )
}

/** Список и раздел стоят рядом: окно расширенное и шире. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ThreePaneScaffoldNavigator<SettingsSection>.besideEachOther(): Boolean =
    scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
        scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
