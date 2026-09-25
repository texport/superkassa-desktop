package kz.mybrain.superkassa.presentation.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.listDetailDirective
import kz.mybrain.superkassa.navigation.settings.SettingsSectionKey

/**
 * Настройки «списком и подробностями» Material 3.
 *
 * Слева — список разделов, справа — настройки открытого раздела одной
 * колонкой. Так Material 3 велит раскладывать настройки на большом экране
 * (Canonical layouts → List-detail): прежние вкладки с карточками столбцами
 * вставали на мониторе вразнобой по высоте.
 *
 * Сколько панелей рядом, решает класс окна ([listDetailDirective]).
 * На расширенном окне и шире обе стоят рядом: выбор раздела подсвечен
 * в списке и шагом истории окна не становится — «назад» уводит из настроек.
 * На телефоне, складном и планшете стоймя раздел открывается поверх списка
 * ключом [SettingsSectionKey] в общей истории окна: назад к списку ведут
 * жест Android, Escape и стрелка в шапке окна — как между разделами.
 * Так устроены настройки Pixel.
 *
 * @param opened раздел, открытый поверх списка историей окна; `null` —
 *   открыт сам список.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsPanes(board: SettingsBoard, opened: SettingsSectionKey?, modifier: Modifier = Modifier) {
    val directive = listDetailDirective()
    val beside = directive.maxHorizontalPartitions > 1
    val choice = settingsChoice(board.sections, opened, beside)
    val role = if (choice.over) ListDetailPaneScaffoldRole.Detail else ListDetailPaneScaffoldRole.List
    ListDetailPaneScaffold(
        directive = directive,
        value = calculateThreePaneScaffoldValue(
            directive.maxHorizontalPartitions,
            ListDetailPaneScaffoldDefaults.adaptStrategies(),
            ThreePaneScaffoldDestinationItem<Any>(role)
        ),
        listPane = {
            AnimatedPane { SettingsList(board, board.sections, choice.open.takeIf { beside }, choice.pick) }
        },
        detailPane = { AnimatedPane { SectionPane(board, choice.open, beside) } },
        modifier = modifier
    )
}
