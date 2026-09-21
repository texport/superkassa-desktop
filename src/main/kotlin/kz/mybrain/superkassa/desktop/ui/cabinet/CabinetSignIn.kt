package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.edsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Вход владельца в кабинет по ЭЦП.
 *
 * Одна кнопка и одна строка объяснения: сертификат владелец выбирает
 * в окне NCALayer, пароль вводит там же. Приложение ключа не видит
 * и никуда его не сохраняет — сюда возвращается только готовая подпись.
 *
 * Пока NCALayer ждёт пароль, на месте кнопки стоит ожидание с видимым
 * сроком и отменой — см. [SignWait]. Прежде там была занятая кнопка,
 * и владелец, уже подписавший в окне NCALayer, три минуты смотрел
 * на неподвижный экран.
 *
 * Способ входа один — по ЭЦП. Вход по набранным ИИН и БИН стоял тут же
 * и предлагал ввести любые двенадцать цифр: на экране входа это вторая,
 * неохраняемая дверь, и владелец видел её первой. Убран целиком, вместе
 * с подпоркой в сеансе: личность владельца приходит только из сертификата.
 */
@Composable
fun CabinetSignIn(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.normal, Alignment.CenterVertically)
    ) {
        ElevatedCard(modifier = Modifier.widthIn(max = Sizes.loginColumn)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                Icon(
                    imageVector = AppIcons.cabinet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizes.headerIcon)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    Text(texts.title, style = MaterialTheme.typography.headlineSmall)
                    InfoTip(texts.hints.signIn)
                }
                SignInAction(cabinet, session.language, texts)
                Text(
                    text = "${texts.address}: ${session.preferences.cabinetUrl}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Главное действие двери: вход — или ожидание подписи на его месте.
 *
 * Ожидание встаёт туда же, где стояла кнопка: владелец прерывает его тем
 * же местом, где начал. Занятая кнопка вместо этого не говорила ни сколько
 * ждать, ни чем прервать.
 */
@Composable
private fun SignInAction(cabinet: CabinetSession, language: Language, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    var waiting by remember { mutableStateOf<Waiting?>(null) }
    val started = waiting
    if (started == null) {
        BusyButton(
            text = if (cabinet.busy) texts.signing else texts.signIn,
            busy = cabinet.busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = { waiting = signIn(scope, cabinet) { waiting = null } }
        )
    } else {
        SignWait(
            left = leftOf(started),
            window = NcaLayer.SIGN_WINDOW,
            texts = texts,
            eds = edsTexts(language),
            onCancel = { started.job?.cancel() }
        )
    }
}

/**
 * Начинает вход и отдаёт начатое ожидание.
 *
 * Ожидание объявляется до запуска работы, а снимается в `finally`: иначе
 * отмена или отказ оставили бы на экране отсчёт, за которым уже никто
 * не ждёт. Отмена сюда приходит обычной отменой сопрограммы — сеанс
 * кабинета снимает по ней занятость сам, и повторять за него нечего.
 */
private fun signIn(scope: CoroutineScope, cabinet: CabinetSession, done: () -> Unit): Waiting {
    val waiting = Waiting(TimeSource.Monotonic.markNow())
    waiting.job = scope.launch {
        try {
            if (cabinet.signIn()) cabinet.refreshRegisters()
        } finally {
            done()
        }
    }
    return waiting
}

/**
 * Сколько ожидания осталось.
 *
 * Отсчёт идёт от нажатия, а срок — тот же, что у [NcaLayer]: считать
 * его здесь своим значением значит однажды показать владельцу время,
 * которого у него нет.
 */
@Composable
private fun leftOf(waiting: Waiting): Duration {
    var left by remember(waiting) { mutableStateOf(NcaLayer.SIGN_WINDOW) }
    LaunchedEffect(waiting) {
        while (isActive) {
            left = (NcaLayer.SIGN_WINDOW - waiting.since.elapsedNow()).coerceAtLeast(Duration.ZERO)
            delay(Durations.everySecond)
        }
    }
    return left
}

/** Начатое ожидание подписи: с какого мгновения идёт и чем прерывается. */
private class Waiting(val since: TimeMark) {
    var job: Job? = null
}
