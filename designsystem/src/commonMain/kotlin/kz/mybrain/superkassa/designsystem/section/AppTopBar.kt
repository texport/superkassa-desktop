package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Шапка окна — одна на всё приложение.
 *
 * У кассы она называет кассу и её состояние, у кабинета — компанию
 * и вошедшего владельца. Прежде у кабинета была своя шапка внутри
 * рабочей области: две шапки друг под другом, разной высоты и с разными
 * полями, а язык и выход у них лежали в разных местах окна.
 *
 * Заголовок, стрелка и значки справа стоят по краям содержимого разделов:
 * поле окна за вычетом поля, которое `TopAppBar` ставит сам
 * ([Spacing.barInset]). Прежде поле окна ложилось поверх него, и заголовок
 * с значками стояли на шестнадцать точек внутрь от краёв содержимого.
 *
 * @param subtitleKept хвост подзаголовка, который не сокращается: имя
 *   кассира. Сокращается то, что перед ним, — кассы или организации.
 * @param lead начало шапки: стрелка назад или кнопка меню;
 *   `null` — заголовок начинается с поля окна.
 * @param actions кнопки справа: у каждого раздела свои.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    subtitleKept: String? = null,
    lead: BarLead? = null,
    actions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        colors = barColors(),
        navigationIcon = { lead?.let { BarLeadIcon(it) } },
        title = {
            val start = if (lead == null) barEdge else Spacing.itemGap
            BarTitle(title, subtitle, subtitleKept, Modifier.padding(start = start))
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = barEdge),
                content = actions
            )
        }
    )
}

/** Заголовок шапки и подзаголовок под ним. */
@Composable
private fun BarTitle(title: String, subtitle: String?, kept: String?, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TopBarSubtitle(subtitle, kept)
    }
}

/** Начало шапки: стрелка или меню — по [lead]. */
@Composable
private fun BarLeadIcon(lead: BarLead) {
    when (lead) {
        is BarLead.Back -> IconButton(onClick = lead.onClick, modifier = Modifier.padding(start = barEdge)) {
            Icon(AppIcons.back, contentDescription = lead.label)
        }
        is BarLead.Menu -> IconButton(onClick = lead.onClick, modifier = Modifier.padding(start = barEdge)) {
            Icon(AppIcons.menu, contentDescription = lead.label)
        }
    }
}

/**
 * Подзаголовок шапки: сокращаемая часть и несокращаемый хвост.
 *
 * Строка одна, как и была; когда места мало, многоточие встаёт в конце
 * названия организации, а имя кассира остаётся целым.
 */
@Composable
private fun TopBarSubtitle(subtitle: String?, kept: String?) {
    val parts = listOfNotNull(subtitle?.takeIf { it.isNotBlank() }, kept?.takeIf { it.isNotBlank() })
    if (parts.isEmpty()) return
    val style = MaterialTheme.typography.bodySmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (!kept.isNullOrBlank()) {
            val lead = if (subtitle.isNullOrBlank()) "" else Glyphs.SEPARATOR
            Text(text = lead + kept, style = style, color = color, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Поле шапки у края окна: поле окна за вычетом поля самой шапки — тогда
 * заголовок, значок стрелки и значки справа встают по краям содержимого.
 */
private val barEdge: Dp
    @Composable get() = (windowMargin - Spacing.barInset).coerceAtLeast(Spacing.flush)

/**
 * Шапка стоит на той же `surface`, что рельс и разделы: своя подложка
 * `surfaceContainer` делала её отдельной серой полосой над окном. Фон
 * шапки прозрачный — под ней фон окна: `TopAppBar` плавно перекрашивает
 * свой фон, и при смене темы шапка отдельным прямоугольником догоняла
 * окно, которое перекрашивается сразу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun barColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent
)

/**
 * Где от края окна начинается кнопка в начале шапки — стрелка или меню:
 * поле самой шапки и поле у её края. Панель разделов, выезжая поверх
 * окна, ставит свою кнопку закрытия ровно сюда — на место кнопки меню.
 */
val barLeadStart: Dp
    @Composable get() = Spacing.barSide + barEdge
