package kz.mybrain.superkassa.presentation.update.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.api.update.UpdateTexts

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
internal fun RailVersion(updates: UpdatesUiState, onOpenUpdate: () -> Unit) {
    val tip = versionTip(updates, textsOf(LocalLanguage.current).update)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tip) } },
        state = rememberTooltipState()
    ) {
        if (updates.available == null) {
            Text(
                text = updates.version.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.fieldGap)
            )
        } else {
            UpdateMark(updates.version.label, tip, onOpenUpdate)
        }
    }
}

/**
 * Подсказка к версии в углу.
 *
 * В углу всегда стоит версия, которая работает сейчас: она нужна кассиру
 * и поддержке. О новой говорят цвет, значок и подсказка — подпись с чужим
 * номером читалась бы как своя.
 */
private fun versionTip(updates: UpdatesUiState, texts: UpdateTexts): String {
    val available = updates.available ?: return "${texts.appName} ${updates.version}"
    return "${texts.appName} ${updates.version}${Glyphs.SEPARATOR}${texts.available} ${available.version}"
}

/** Подпись версии, ставшая кнопкой: новая версия ждёт. */
@Composable
private fun UpdateMark(current: String, description: String, onOpen: () -> Unit) {
    TextButton(onClick = onOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
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
