package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.designsystem.wizard.WizardPage
import kz.mybrain.superkassa.designsystem.wizard.WizardProgress
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.presentation.setup.step.StepContent
import kz.mybrain.superkassa.presentation.setup.step.headingOf
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Один шаг мастера подключения на весь раздел.
 *
 * Владелец видел все шаги сразу — выбор пути, номер, кабинет, учёт, пин
 * с полями — и не понимал, с чего начать. Теперь шаг один: иллюстрация,
 * что происходит и зачем, ровно те поля, что нужны сейчас, и «Далее».
 * Шаги — записи истории окна: «Назад» под шагом, жест, Escape и стрелка
 * в шапке ведут на предыдущий шаг одинаково.
 *
 * Первый шаг — сам раздел «Новая касса»; открытый заново, он сразу
 * выкладывает поверх себя шаги до того, на котором мастер бросили. Шаг,
 * на котором стоять нельзя, — история пережила «Начать заново» или
 * выгрузку приложения, — снимается сам.
 *
 * @param step имя шага поверх первого; `null` — первый шаг.
 */
@Composable
fun SetupContent(parts: SetupParts, step: String? = null) {
    val state = parts.state
    val route = state.route
    val shown = if (step == null) route.first else SetupStep.entries.firstOrNull { it.name == step }
    val opens = shown != null && route.opens(shown, state.draft)
    if (step == null) ResumeOnce(route, state.draft) else CloseWhenStale(step, opens)
    if (shown == null || !opens) return
    WatchRecord(parts)
    if (state.startingOver) StartOverDialog(parts.actions)
    val setup = textsOf(LocalLanguage.current).setup
    val number = route.number(shown)
    val count = route.steps.size
    WizardPage(
        progress = WizardProgress(number, count, setup.stepOf.fill(number, count)),
        heading = headingOf(shown, state.way, setup),
        leading = { StepBack(parts, root = step == null, setup) },
        trailing = { StepForward(parts, shown, setup) }
    ) {
        StepContent(shown, parts)
    }
}

/**
 * Касса в кабинете перечитывается, пока номера КГД нет.
 *
 * Ответа КГД ждут на шаге учёта, а кнопка «Завести кассу» ждёт его же:
 * владелец не должен открывать мастер заново, чтобы увидеть ответ.
 */
@Composable
private fun WatchRecord(parts: SetupParts) {
    val office = parts.office ?: return
    if (parts.state.way != SetupWay.ViaCabinet) return
    val registerId = parts.state.draft.cabinetRegisterId
    val open = office.session.open
    LaunchedEffect(registerId, open) {
        if (registerId == null || !open) return@LaunchedEffect
        office.actions.readRecord(registerId)
        office.actions.watchRecord(registerId, Durations.whileWatching)
    }
}
