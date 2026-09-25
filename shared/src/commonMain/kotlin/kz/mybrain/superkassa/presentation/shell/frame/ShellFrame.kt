package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.keyboard.clearFocusOnTap
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.shell.section.Destination

/**
 * Рамка окна — одна на всё приложение: навигация по разделам, шапка
 * и сообщения.
 *
 * Раскладка `NavigationSuiteScaffoldLayout` Material 3 ставит навигацию по
 * классу окна ([ShellNavigation]): полосу — снизу, рельс ([SectionRail]) —
 * слева; внутри — один `Scaffold` с одной шапкой и одним местом для снекбара.
 * Все разделы с названиями открываются кнопкой меню поверх окна
 * ([SectionDrawer]). Разделы своей навигации, шапок и отступов под них
 * не строят. Рамка одна и у рабочего окна, и у окна до входа —
 * меняется только набор разделов ([Destination]).
 *
 * @param marked разделы с отметкой — например, настройки, когда вышла
 *   новая версия кассы.
 * @param topBar шапка окна; ей отдаётся кнопка меню, когда разделы
 *   спрятаны в панель поверх окна, — иначе `null`.
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
    val window = LocalWindowClass.current
    val navigation = ShellNavigation.of(window, sections.size)
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val close = { scope.launch { drawer.close() }.let { } }
    val open = { scope.launch { drawer.open() }.let { } }
    val items = SectionItems(sections, current, marked) { picked -> onPick(picked).also { close() } }
    // Кнопка меню у рельса — только на большом окне: там разделов с их
    // названиями в строку ищут чаще, чем на планшете у кассы.
    val railMenu = navigation.railed && window.width >= WidthClass.Large
    val onMenu = open.takeIf { navigation == ShellNavigation.Modal }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.clearFocusOnTap()) {
        SectionDrawer(drawer, fromRail = railMenu, enabled = onMenu != null || railMenu, close, items) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = { NavigationOf(navigation, items, open.takeIf { railMenu }) },
                navigationSuiteType = navigation.type
            ) {
                Scaffold(topBar = { topBar(onMenu) }, snackbarHost = snackbarHost, content = content)
            }
        }
    }
}

/** Навигация окна: рельс со своей кнопкой меню или полоса Material 3 снизу. */
@Composable
private fun NavigationOf(navigation: ShellNavigation, items: SectionItems<*>, onMenu: (() -> Unit)?) {
    if (navigation.railed) {
        SectionRail(onMenu) { items(navigation.type) }
    } else {
        NavigationSuite(navigationSuiteType = navigation.type) { items(navigation.type) }
    }
}

/** Разделы пунктами навигации нужного вида. */
internal class SectionItems<D : Destination>(
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

    /** Разделы пунктами панели поверх окна: значок и название в строку. */
    @Composable
    fun drawer() {
        val texts = LocalStrings.current.sections
        sections.forEach { section ->
            NavigationDrawerItem(
                label = { Text(section.title(texts)) },
                selected = section == current,
                onClick = { onPick(section) },
                icon = { Icon(section.icon, contentDescription = null) },
                badge = if (section in marked) ({ Badge() }) else null,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .padding(start = Spacing.drawerItemNudge)
            )
        }
    }
}
