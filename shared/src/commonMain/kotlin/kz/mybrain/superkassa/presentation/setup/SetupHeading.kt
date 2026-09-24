package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.section.ScreenTitle
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.setup.SetupTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Название мастера, возврат и «Начать заново».
 *
 * Пока мастер ничего не прошёл, забывать нечего, и кнопки нет: нажатая
 * по ошибке, она стирает заводской номер, уже унесённый в кабинет.
 * Что мастер можно бросить и продолжить, сказано подсказкой у названия,
 * а не второй строкой шапки.
 */
@Composable
internal fun SetupHeading(setup: SetupTexts, started: Boolean, actions: SetupActions, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        onBack?.let { back ->
            IconButton(onClick = back) {
                Icon(AppIcons.back, contentDescription = LocalStrings.current.settings.back)
            }
        }
        ScreenTitle(setup.title, modifier = Modifier.weight(1f, fill = false))
        InfoTip(setup.explain)
        Spacer(modifier = Modifier.weight(1f))
        if (started) {
            TextButton(onClick = { actions.askStartOver(true) }) { Text(setup.startOver) }
        }
    }
}
