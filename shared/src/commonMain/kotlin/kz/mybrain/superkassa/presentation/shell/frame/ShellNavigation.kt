package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import kz.mybrain.superkassa.designsystem.adaptive.HeightClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.adaptive.WindowClass

/**
 * Как окно показывает разделы — по рекомендации Material 3 для
 * `NavigationSuiteScaffold`.
 *
 * Телефон и планшет стоймя (компактная и средняя ширина) и низкое окно —
 * нижняя полоса: в ней помещается от трёх до пяти разделов, и у кассира
 * их ровно пять. На планшете стоймя рельс отнимал ширину у работы, которой
 * там и так мало; Google по умолчанию ставит там рельс, но Material 3
 * допускает полосу и для средней ширины — с подписями сбоку от значков. Если
 * разделов больше, как у администратора, полосы нет: все разделы — в
 * модальном широком рельсе по кнопке меню в шапке, как велит гайдлайн для
 * больше чем пяти пунктов. Шире — узкий рельс с подписями под значками,
 * как у Google по умолчанию: ширина остаётся работе. На большом окне
 * кнопка меню в шапке рельса открывает все разделы поверх окна тем же
 * модальным широким рельсом, что на телефоне. Развёрнутый рельс в
 * раскладке — не меньше 220 точек по Material 3 — отодвигал работу
 * вправо на пустую полосу за короткими названиями разделов.
 *
 * @property type вид навигации для `NavigationSuiteScaffold`; у модального
 *   рельса своего места в раскладке нет.
 */
internal enum class ShellNavigation(val type: NavigationSuiteType) {
    Bar(NavigationSuiteType.ShortNavigationBarCompact),
    LowBar(NavigationSuiteType.ShortNavigationBarMedium),
    Rail(NavigationSuiteType.WideNavigationRailCollapsed),
    Modal(NavigationSuiteType.None);

    /** Рельс стоит сбоку окна: его разделы прокручиваются, когда им тесно. */
    val railed: Boolean get() = this == Rail

    companion object {
        /** Сколько разделов помещается в нижнюю полосу по Material 3. */
        const val BAR_MAX = 5

        /** Навигация для окна [window] с [count] разделами. */
        fun of(window: WindowClass, count: Int): ShellNavigation = when {
            window.width == WidthClass.Compact -> if (count <= BAR_MAX) Bar else Modal
            window.width == WidthClass.Medium || window.height == HeightClass.Compact ->
                if (count <= BAR_MAX) LowBar else Modal
            else -> Rail
        }
    }
}
