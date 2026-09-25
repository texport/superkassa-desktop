package kz.mybrain.superkassa.presentation.setup

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.domain.setup.model.SetupRoute
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.step.SetupStepKey
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Второстепенное действие шага: «Назад» — или «Начать заново» на первом.
 *
 * «Назад» — тот же шаг по истории окна, что жест и стрелка в шапке.
 * На первом шаге назад некуда, и там живёт «Начать заново»: только когда
 * есть что забывать, — нажатая по ошибке, она стёрла бы номер, уже
 * унесённый в кабинет, поэтому ещё и спрашивает.
 *
 * @param root это первый шаг мастера.
 */
@Composable
internal fun StepBack(parts: SetupParts, root: Boolean, setup: SetupTexts) {
    val navigator = LocalNavigator.current
    when {
        !root -> TextButton(onClick = navigator::back) { Text(setup.back) }
        parts.state.draft.factoryNumber != null ->
            TextButton(onClick = { parts.actions.askStartOver(true) }) { Text(setup.startOver) }
    }
}

/**
 * Главное действие шага: «Далее», а на последнем — «Завести кассу».
 *
 * «Далее» оживает, когда шаг сделан; выбор пути запоминается, как только
 * по нему пошли, — мастер, продолженный назавтра, идёт тем же путём.
 */
@Composable
internal fun StepForward(parts: SetupParts, step: SetupStep, setup: SetupTexts) {
    val state = parts.state
    val next = state.route.after(step) ?: return ConnectButton(parts, setup)
    val navigator = LocalNavigator.current
    Button(
        enabled = state.done(step, parts.onRecord),
        onClick = {
            if (step == SetupStep.Way) parts.actions.chooseWay(state.way)
            navigator.open(SetupStepKey(next.name))
        }
    ) { Text(setup.next) }
}

/**
 * «Завести кассу» — последнее действие мастера.
 *
 * Заведённая касса уводит туда, откуда пришли, — к кассам окна до входа,
 * где её уже видно; в разделе рабочего окна мастер возвращается к первому
 * шагу: пройденное забыто, и следующая касса начнётся с начала.
 */
@Composable
private fun ConnectButton(parts: SetupParts, setup: SetupTexts) {
    val navigator = LocalNavigator.current
    val state = parts.state
    val session = parts.office?.session
    val second = state.route.after(state.route.first)
    BusyButton(
        text = setup.connect,
        busy = state.form.busy || session?.busy == true,
        enabled = state.canConnect(parts.onRecord, session?.open == true)
    ) {
        parts.actions.connect {
            parts.onBack?.invoke() ?: second?.let { navigator.close(SetupStepKey(it.name)) }
        }
    }
}

/**
 * Продолжить брошенный мастер: один раз за открытие раздела выложить
 * поверх первого шага все шаги до того, на котором остановились.
 *
 * Шаги ложатся в историю, а не перескакиваются: «назад» с продолженного
 * шага ведёт на предыдущий, а не из мастера. Второй раз — после возврата
 * «назад» на первый шаг — не выкладываются: отметка живёт в записи
 * истории первого шага и переживает поворот экрана.
 */
@Composable
internal fun ResumeOnce(route: SetupRoute, draft: KkmSetupDraft) {
    val navigator = LocalNavigator.current
    var resumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (resumed) return@LaunchedEffect
        resumed = true
        route.toResume(draft).forEach { navigator.open(SetupStepKey(it.name)) }
    }
}

/**
 * Снять шаг [step], на котором стоять нельзя.
 *
 * Снимается именно этот шаг, а не «шаг назад»: пока снятый уходит
 * с экрана, он ещё нарисован, и второй «назад» увёл бы из раздела.
 */
@Composable
internal fun CloseWhenStale(step: String, opens: Boolean) {
    val navigator = LocalNavigator.current
    LaunchedEffect(opens) { if (!opens) navigator.close(SetupStepKey(step)) }
}
