package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Вопрос перед тем, как забыть пройденное.
 *
 * Нажатие стирало пройденное сразу: заводской номер, уже унесённый
 * в кабинет, и кассу, заведённую там под ним.
 */
@Composable
internal fun StartOverDialog(actions: SetupActions) {
    val language = LocalLanguage.current
    val setup = textsOf(language).setup
    ConfirmDangerDialog(
        what = setup.startOverAsk,
        explain = setup.startOverExplain,
        action = setup.startOver,
        cancel = textsOf(language).kassa.money.drawer.cancel,
        onCancel = { actions.askStartOver(false) },
        onConfirm = actions::startOver
    )
}
