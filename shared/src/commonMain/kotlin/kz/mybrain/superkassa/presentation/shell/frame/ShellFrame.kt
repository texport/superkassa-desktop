package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalWideNavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.shell.section.Destination

/**
 * Рамка окна — одна на всё приложение: навигация по разделам, шапка
 * и сообщения.
 *
 * `NavigationSuiteScaffold` Material 3 сам ставит навигацию по классу окна
 * ([ShellNavigation]), а внутри него — один `Scaffold` с одной шапкой
 * и одним местом для снекбара. Разделы своей навигации, шапок и отступов
 * под них не строят. Рамка одна и у рабочего окна, и у окна до входа —
 * меняется только набор разделов ([Destination]).
 *
 * @param marked разделы с отметкой — например, настройки, когда вышла
 *   новая версия кассы.
 * @param topBar шапка окна; ей отдаётся кнопка меню, когда разделы
 *   спрятаны в модальный рельс, — иначе `null`.
 */
@Composable
internal fun <D : Destination> ShellFrame(
    sections: List<D>,
    current: D,
    onPick: (D) -> Unit,
    marked: Set<D> = emptySet(),
    topBar: @Composable (onMenu: (() -> Unit)?) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val window = LocalWindowClass.current
    val navigation = ShellNavigation.of(window, sections.size, expanded)
    val modal = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val items = SectionItems(sections, current, marked) { picked ->
        onPick(picked)
        scope.launch { modal.collapse() }
    }
    val onMenu: (() -> Unit)? = if (navigation == ShellNavigation.Modal) ({ scope.launch { modal.expand() } }) else null
    val toggle = window.width >= WidthClass.Large
    Box {
        NavigationSuiteScaffold(
            navigationItems = { NavigationItems(navigation, items) },
            navigationSuiteType = navigation.type,
            containerColor = MaterialTheme.colorScheme.surface,
            primaryActionContent = { if (navigation.railed && toggle) RailToggle(expanded) { expanded = !expanded } }
        ) {
            Scaffold(topBar = { topBar(onMenu) }, snackbarHost = snackbarHost, content = content)
        }
        if (onMenu != null) ModalSections(modal, items)
    }
}

/** Все разделы в модальном широком рельсе: на телефоне, где их больше пяти. */
@Composable
private fun ModalSections(state: WideNavigationRailState, items: SectionItems<*>) {
    ModalWideNavigationRail(state = state, hideOnCollapse = true) {
        Scrolled { items(NavigationSuiteType.WideNavigationRailExpanded) }
    }
}

/** Пункты навигации окна: в рельсе — прокручиваемым столбцом, в полосе — рядом. */
@Composable
private fun NavigationItems(navigation: ShellNavigation, items: SectionItems<*>) {
    if (navigation.railed) {
        Scrolled { items(navigation.type) }
    } else {
        items(navigation.type)
    }
}

/** Разделы пунктами навигации нужного вида. */
private class SectionItems<D : Destination>(
    val sections: List<D>,
    val current: D,
    val marked: Set<D>,
    val onPick: (D) -> Unit
) {
    @Composable
    operator fun invoke(type: NavigationSuiteType) {
        val texts = LocalStrings.current.sections
        sections.forEach { section ->
            NavigationSuiteItem(
                selected = section == current,
                onClick = { onPick(section) },
                icon = { Icon(section.icon, contentDescription = null) },
                label = { Text(section.title(texts)) },
                navigationSuiteType = type,
                badge = if (section in marked) ({ Badge() }) else null
            )
        }
    }
}

/**
 * Разделы рельса прокручиваются: у администратора их десять, а окно кассы
 * бывает ростом в 640 точек — на ноутбуке и на экране прилавка. Рельс
 * Material 3 сам не прокручивается, и нижние разделы уходили бы за край.
 */
@Composable
private fun Scrolled(content: @Composable () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
}

/** Кнопка меню в шапке рельса: развернуть или свернуть его на большом окне. */
@Composable
private fun RailToggle(expanded: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current.general
    IconButton(onClick = onToggle) {
        Icon(AppIcons.menu, contentDescription = if (expanded) texts.collapse else texts.expand)
    }
}
