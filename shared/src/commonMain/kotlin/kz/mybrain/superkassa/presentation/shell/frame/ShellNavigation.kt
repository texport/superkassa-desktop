package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import kz.mybrain.superkassa.designsystem.adaptive.HeightClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.adaptive.WindowClass

/**
 * Как окно показывает разделы — по рекомендации Material 3 для
 * `NavigationSuiteScaffold`.
 *
 * Узкое или низкое окно — телефон стоймя и лёжа — нижняя полоса: в ней
 * помещается от трёх до пяти разделов, и у кассира их ровно пять. Если
 * разделов больше, как у администратора, полосы нет: все разделы — в
 * модальном широком рельсе по кнопке меню в шапке, как велит гайдлайн для
 * больше чем пяти пунктов. Шире — узкий рельс с подписями под значками,
 * как у Google по умолчанию: ширина остаётся работе. На большом окне
 * кнопка меню в шапке рельса разворачивает его с подписями в строку.
 *
 * @property type вид навигации для `NavigationSuiteScaffold`; у модального
 *   рельса своего места в раскладке нет.
 */
internal enum class ShellNavigation(val type: NavigationSuiteType) {
    Bar(NavigationSuiteType.ShortNavigationBarCompact),
    LowBar(NavigationSuiteType.ShortNavigationBarMedium),
    Rail(NavigationSuiteType.WideNavigationRailCollapsed),
    WideRail(NavigationSuiteType.WideNavigationRailExpanded),
    Modal(NavigationSuiteType.None);

    /** Рельс стоит сбоку окна: его разделы прокручиваются, когда им тесно. */
    val railed: Boolean get() = this == Rail || this == WideRail

    companion object {
        /** Сколько разделов помещается в нижнюю полосу по Material 3. */
        const val BAR_MAX = 5

        /**
         * Навигация для окна [window] с [count] разделами.
         *
         * @param expanded рельс большого окна развёрнут.
         */
        fun of(window: WindowClass, count: Int, expanded: Boolean): ShellNavigation = when {
            window.width == WidthClass.Compact -> if (count <= BAR_MAX) Bar else Modal
            window.height == HeightClass.Compact -> if (count <= BAR_MAX) LowBar else Modal
            window.width >= WidthClass.Large && expanded -> WideRail
            else -> Rail
        }
    }
}
