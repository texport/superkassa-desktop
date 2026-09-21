package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Заголовок карточки со стрелкой: сворачивает и разворачивает её часть.
 *
 * Высота кассовой колонки — общий ресурс: пока кассир набирает товар,
 * реквизиты и деньги ему не нужны, а когда рассчитывается — наоборот.
 * Свёрнутое остаётся на экране заголовком, а не исчезает: кассир видит,
 * что раздел на месте, и разворачивает его одним нажатием.
 *
 * @param title название раздела.
 * @param expanded развёрнут ли раздел сейчас.
 * @param onToggle переключение состояния.
 * @param info объяснение раздела — тем же именем и смыслом, что
 *   у `SectionCard(info = ...)`: значок у заголовка, под ним предмет
 *   раздела. Свёрнутому разделу оно нужнее, чем развёрнутому: содержимого
 *   не видно, и по одному названию не решить, стоит ли раскрывать.
 * @param trailing то, что видно и в свёрнутом виде справа от названия.
 * @param always то, что остаётся на экране и свёрнутым: главное в разделе.
 * @param content содержимое, которое прячется.
 */
@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    info: String? = null,
    trailing: @Composable () -> Unit = {},
    always: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        SectionHeader(title, expanded, onToggle, info, trailing)
        always()
        Collapsible(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug), content = content)
        }
    }
}

/**
 * Строка заголовка со стрелкой.
 *
 * Отдельно от [CollapsibleSection] для карточек, где спрятанное лежит
 * по обе стороны от постоянно видимого: в денежном блоке итог стоит
 * между оплатой и сдачей и не исчезает ни при каком состоянии.
 */
@Composable
fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    info: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        // Название и значок объяснения идут вместе: значок, отданный
        // правому краю, читался бы как подсказка к тому, что стоит справа.
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            SectionTitle(title, Modifier.weight(1f, fill = false))
            info?.let { InfoTip(it) }
        }
        trailing()
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) AppIcons.collapse else AppIcons.expand,
                contentDescription = if (expanded) texts.common.collapse else texts.common.expand
            )
        }
    }
}

/** Появление и исчезновение свёрнутой части — одно на все карточки. */
@Composable
fun Collapsible(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        content()
    }
}
