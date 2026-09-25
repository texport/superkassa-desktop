package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.designsystem.theme.size.Panes

/**
 * Как делить окно на панели «списка и подробностей» — по классу окна.
 *
 * Правило Material 3 (Canonical layouts → List-detail): на компактном
 * и среднем окне одна панель — подробности сменяют список, на расширенном
 * и шире — две рядом. Класс берётся из [LocalWindowClass], а не меряется
 * заново: окно меряет корень один раз, и у всех разделов оно одного класса.
 * Зазор между панелями и ширина списка — общие токены [Panes].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun listDetailDirective(): PaneScaffoldDirective {
    val width = LocalWindowClass.current.width
    return remember(width) {
        PaneScaffoldDirective(
            maxHorizontalPartitions = if (width >= WidthClass.Expanded) BESIDE else ALONE,
            horizontalPartitionSpacerSize = Panes.gap,
            maxVerticalPartitions = ALONE,
            verticalPartitionSpacerSize = Panes.gap,
            defaultPanePreferredWidth = Panes.list,
            excludedBounds = emptyList()
        )
    }
}

/**
 * Что показывает «список и подробности»: при одной панели — подробности,
 * если [over], иначе список; при двух — обе.
 *
 * Выбор держит история окна, а не навигатор панелей: подробности поверх
 * списка — шаг общей истории, и «назад» у окна одно.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun listDetailValue(directive: PaneScaffoldDirective, over: Boolean): ThreePaneScaffoldValue =
    calculateThreePaneScaffoldValue(
        directive.maxHorizontalPartitions,
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
        ThreePaneScaffoldDestinationItem<Any>(
            if (over) ListDetailPaneScaffoldRole.Detail else ListDetailPaneScaffoldRole.List
        )
    )

/** Одна панель: подробности сменяют список. */
private const val ALONE = 1

/** Две панели рядом: список и подробности. */
private const val BESIDE = 2
