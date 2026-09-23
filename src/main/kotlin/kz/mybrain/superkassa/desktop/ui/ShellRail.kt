package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.adaptive.LocalWindowClass
import kz.mybrain.superkassa.desktop.ui.adaptive.WidthClass
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Рельс разделов.
 *
 * Ширина считается по самой длинной подписи набора: у Material она
 * постоянная, и «Новая касса» упиралась в край окна. Считается так же,
 * как ширина сегментов, — одним правилом на весь интерфейс.
 *
 * Разделы прокручиваются, а версия под ними стоит на месте: администратору
 * их десяток, а окно кассы бывает ростом в 700 точек — на ноутбуке и на
 * экране прилавка. Без прокрутки «Настройки» уходили под нижний край
 * вместе с версией, и открыть их было нечем.
 *
 * Рельс остаётся рельсом при любой ширине окна. Material 3 в компактном
 * окне предлагает нижнюю полосу, но в ней помещается от трёх до пяти
 * разделов, а у администратора их десять; прятать разделы в выдвижное
 * меню значит прятать рабочие вкладки кассира за лишним нажатием.
 * Свёрнутый рельс шириной в значок держит все разделы на виду
 * и отнимает у экрана меньше, чем подписи.
 *
 * @param footer то, что стоит в нижнем углу рельса под разделами:
 *   версия кассы и знак о новой.
 */
@Composable
internal fun SectionRail(
    sections: List<Section>,
    current: Section,
    collapsed: Boolean,
    onToggle: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    onPick: (Section) -> Unit
) {
    val texts = LocalStrings.current
    // В компактном окне рельс всегда свёрнут: подписи съели бы треть ширины.
    // Выбор кассира при этом не трогается — шире окно, и рельс снова такой,
    // каким его оставили.
    val compact = LocalWindowClass.current.width == WidthClass.Compact
    val folded = collapsed || compact
    val railWidth = railWidthFor(sections.map { it.title(texts.sections) })
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(if (folded) Sizes.rail else maxOf(railWidth, Sizes.rail)),
        header = { if (!compact) RailToggle(collapsed, onToggle) }
    ) {
        RailSections(sections, current, folded, onPick)
        footer()
    }
}

/**
 * Ширина развёрнутого рельса по самой длинной подписи.
 *
 * Подписи меряются один раз на набор и язык, а не на каждую перерисовку:
 * при растягивании окна разметка пересчитывается десятки раз в секунду,
 * и раскладка шрифта на каждый такой проход — работа впустую.
 */
@Composable
private fun railWidthFor(titles: List<String>): Dp {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    return remember(titles, labelStyle, density) {
        val widest = titles.maxOfOrNull { measurer.measure(it, labelStyle).size.width } ?: 0
        with(density) { widest.toDp() } + Spacing.roomy * 2
    }
}

/**
 * Разделы рельса с видимой полосой прокрутки.
 *
 * Разделы прокручивались и раньше, но ничем этого не выдавали: «Настройки»
 * стояли за нижним краем, и кассир считал, что их нет. Полоса та же,
 * что у списков, и видна, только когда разделам тесно.
 */
@Composable
private fun ColumnScope.RailSections(
    sections: List<Section>,
    current: Section,
    folded: Boolean,
    onPick: (Section) -> Unit
) {
    val scroll = rememberScrollState()
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            RailItems(sections, current, folded, onPick)
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

/**
 * Кнопка сворачивания рельса.
 *
 * Свёрнутый рельс отдаёт ширину чеку: значки кассир знает наизусть,
 * а подписи нужны первую неделю.
 */
@Composable
private fun RailToggle(collapsed: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = AppIcons.menu,
            contentDescription = if (collapsed) texts.common.expand else texts.common.collapse
        )
    }
}

/** Сами разделы: значок и, если рельс развёрнут, подпись. */
@Composable
private fun RailItems(sections: List<Section>, current: Section, folded: Boolean, onPick: (Section) -> Unit) {
    val texts = LocalStrings.current
    sections.forEach { entry ->
        NavigationRailItem(
            selected = current == entry,
            onClick = { onPick(entry) },
            icon = { Icon(entry.icon, contentDescription = entry.title(texts.sections)) },
            label = if (folded) null else ({ Text(entry.title(texts.sections)) })
        )
    }
}
