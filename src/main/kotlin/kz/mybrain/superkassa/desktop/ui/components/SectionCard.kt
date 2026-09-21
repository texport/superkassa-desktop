package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Карточка раздела: заголовок, дополнение справа и содержимое под ними.
 *
 * Раздел из карточки, столбца, поля и заголовка набирался на каждом экране
 * заново, и они разъехались: где-то заголовок стоял под содержимым, где-то
 * внутреннее поле было вдвое уже. Здесь эта раскладка объявлена один раз.
 *
 * Счётчика строк у заголовка нет намеренно: короткий список владелец
 * пересчитывает глазами, а число рядом с названием читается как
 * оторванная от всего цифра. Там, где длину списка глазами не увидеть,
 * она сказана словами под ним — рядом с кнопкой подгрузки.
 *
 * @param trailing то, что стоит в строке заголовка справа: состояние
 *   или объяснение раздела.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    info: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                SectionTitle(title)
                // Объяснение раздела живёт под значком у заголовка, а не
                // абзацем под ним: абзац читают один раз, а место он занимает
                // всегда. Значок — общий для всех карточек, второго не заводим.
                info?.let { InfoTip(it) }
                Spacer(Modifier.weight(1f))
                trailing()
            }
            content()
        }
    }
}

/**
 * Карточка раздела, которую можно свернуть.
 *
 * Отличается от [SectionCard] только стрелкой в заголовке: длинная
 * карточка собирается из таких разделов, и свёрнутое остаётся на экране
 * строкой заголовка, а не исчезает без следа.
 *
 * Объяснение раздела задаётся тем же `info`, что и у [SectionCard]:
 * свёрнутый раздел — это одна строка названия, и без объяснения владелец
 * раскрывает его, чтобы узнать, что там.
 */
@Composable
fun CollapsibleCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.normal)) {
            CollapsibleSection(
                title = title,
                expanded = expanded,
                onToggle = onToggle,
                info = info,
                trailing = trailing,
                content = content
            )
        }
    }
}

/**
 * Название части раздела и объяснение к ней.
 *
 * Внутри карточки бывает своя часть со своим заголовком: токен, адрес
 * по шагам, работа этой кассы на этой машине. Строка «название и значок
 * подсказки» набиралась в каждой из них заново — здесь она объявлена
 * один раз, и подсказка везде встаёт на одно место.
 */
@Composable
fun SubsectionTitle(title: String, info: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        info?.let { InfoTip(it) }
    }
}

/** Название раздела — одной строкой во всех карточках приложения. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
