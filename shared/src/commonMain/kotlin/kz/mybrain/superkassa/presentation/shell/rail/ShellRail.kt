package kz.mybrain.superkassa.presentation.shell.rail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.LocalWindowClass
import kz.mybrain.superkassa.presentation.common.adaptive.WidthClass
import kz.mybrain.superkassa.presentation.common.list.ColumnScrollbar
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Рельс разделов.
 *
 * Ширина постоянная, как у Material 3 ([Sizes.rail]): прежде она считалась
 * по самой длинной подписи и плавала от роли и языка — содержимое раздела
 * начиналось то со 110, то со 138 точек. Длинная подпись переносится.
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
    // В компактном окне рельс всегда свёрнут: подписи съели бы треть ширины.
    // Выбор кассира при этом не трогается — шире окно, и рельс снова такой,
    // каким его оставили.
    val compact = LocalWindowClass.current.width == WidthClass.Compact
    val folded = collapsed || compact
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(Sizes.rail),
        header = { if (!compact) RailToggle(collapsed, onToggle) }
    ) {
        RailSections(sections, current, folded, onPick)
        // Прокрученные разделы кончаются над версией с зазором: вплотную
        // обрезанная подпись последнего раздела читалась наехавшей на номер.
        Spacer(Modifier.height(Spacing.cardGap))
        footer()
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
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            RailItems(sections, current, folded, onPick)
        }
        ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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
