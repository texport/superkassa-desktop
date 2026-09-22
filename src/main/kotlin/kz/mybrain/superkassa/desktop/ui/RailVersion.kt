package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.updateTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Версия кассы в углу рельса разделов.
 *
 * Видна всегда и мелко: поддержка первым делом спрашивает версию,
 * а искать её в настройках кассир не станет. Полное имя — в подсказке.
 *
 * Когда вышла новая версия, тот же угол становится кнопкой: подпись
 * окрашивается в основной цвет и получает значок, а нажатие открывает
 * окно с предложением скачать. Отдельного места под уведомление нет
 * намеренно — касса и так говорит с кассиром из одного угла.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RailVersion(session: Session, onOpenUpdate: () -> Unit) {
    val texts = updateTexts(session.language)
    val updates = session.updates
    val available = updates.available
    // В углу всегда стоит версия, которая работает сейчас: она нужна
    // кассиру и поддержке. О новой говорят цвет, значок и подсказка —
    // подпись с чужим номером читалась бы как своя.
    val tip = if (available == null) {
        "${texts.appName} ${updates.version}"
    } else {
        "${texts.appName} ${updates.version}${Glyphs.SEPARATOR}${texts.available} ${available.version}"
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tip) } },
        state = rememberTooltipState()
    ) {
        if (available == null) {
            Text(
                text = updates.version.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.snug)
            )
        } else {
            UpdateMark(updates.version.label, tip, onOpenUpdate)
        }
    }
}

/** Подпись версии, ставшая кнопкой: новая версия ждёт. */
@Composable
private fun UpdateMark(current: String, description: String, onOpen: () -> Unit) {
    TextButton(onClick = onOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            Icon(
                imageVector = AppIcons.update,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = current,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
