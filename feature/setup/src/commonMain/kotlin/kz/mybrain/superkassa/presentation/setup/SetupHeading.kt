package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.section.ScreenTitle
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Название мастера и «Начать заново».
 *
 * Возврата здесь нет: стрелку назад рисует шапка окна — и в разделе
 * кассы, и за дверью экрана входа, — а мастер своей не строит.
 *
 * Пока мастер ничего не прошёл, забывать нечего, и кнопки нет: нажатая
 * по ошибке, она стирает заводской номер, уже унесённый в кабинет.
 * Что мастер можно бросить и продолжить, сказано подсказкой у названия,
 * а не второй строкой шапки.
 */
@Composable
internal fun SetupHeading(setup: SetupTexts, started: Boolean, actions: SetupActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreenTitle(setup.title, modifier = Modifier.weight(1f, fill = false))
        InfoTip(setup.explain)
        Spacer(modifier = Modifier.weight(1f))
        if (started) {
            TextButton(onClick = { actions.askStartOver(true) }) { Text(setup.startOver) }
        }
    }
}
