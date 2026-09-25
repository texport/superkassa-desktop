package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Шапка окна — одна на всё приложение.
 *
 * У кассы она называет кассу и её состояние, у кабинета — компанию
 * и вошедшего владельца. Прежде у кабинета была своя шапка внутри
 * рабочей области: две шапки друг под другом, разной высоты и с разными
 * полями, а язык и выход у них лежали в разных местах окна.
 *
 * Заголовок и действия отступают от краёв окна теми же полями, что
 * и содержимое разделов: иначе название начинается от самого края стекла.
 *
 * @param subtitleKept хвост подзаголовка, который не сокращается: имя
 *   кассира. Сокращается то, что перед ним, — кассы или организации.
 * @param lead начало шапки: стрелка назад, кнопка меню или значок;
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
    // Цвета — Material 3 по умолчанию: шапка стоит на той же `surface`,
    // что рельс и разделы. Своя подложка `surfaceContainer` делала её
    // отдельной серой полосой над окном, чужой рельсу и содержимому.
    TopAppBar(
        navigationIcon = { lead?.let { BarLeadIcon(it) } },
        title = {
            Column(modifier = Modifier.padding(start = if (lead == null) windowMargin else Spacing.itemGap)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TopBarSubtitle(subtitle, subtitleKept)
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = windowMargin),
                content = actions
            )
        }
    )
}

/** Начало шапки: стрелка, меню или значок — по [lead]. */
@Composable
private fun BarLeadIcon(lead: BarLead) {
    when (lead) {
        is BarLead.Back -> IconButton(onClick = lead.onClick, modifier = Modifier.padding(start = Spacing.fieldGap)) {
            Icon(AppIcons.back, contentDescription = lead.label)
        }
        is BarLead.Menu -> IconButton(onClick = lead.onClick, modifier = Modifier.padding(start = Spacing.fieldGap)) {
            Icon(AppIcons.menu, contentDescription = lead.label)
        }
        is BarLead.Badge -> TopBarBadge(lead.icon)
    }
}

/**
 * Опознавательный значок в кружке.
 *
 * Своя разметка вместо готового составного: у Material 3 нет отдельного
 * элемента для такого значка — гайдлайн описывает его содержимым
 * `navigationIcon` и оставляет вид на усмотрение приложения.
 */
@Composable
private fun TopBarBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .padding(start = windowMargin)
            .size(Sizes.headerIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
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
