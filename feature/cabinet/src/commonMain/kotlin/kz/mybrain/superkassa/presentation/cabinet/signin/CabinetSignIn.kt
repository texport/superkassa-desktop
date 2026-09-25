package kz.mybrain.superkassa.presentation.cabinet.signin

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.component.SignWait
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.time.Duration
import kotlin.time.TimeMark

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
 * Способ входа один — по ЭЦП: личность владельца приходит только
 * из сертификата, второй неохраняемой двери у кабинета нет.
 */
@Composable
internal fun CabinetSignIn(
    state: CabinetUiState,
    language: Language,
    texts: CabinetTexts,
    actions: CabinetActions
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap, Alignment.CenterVertically)
    ) {
        ElevatedCard(modifier = Modifier.widthIn(max = Sizes.loginColumn)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.blockPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
            ) {
                DoorTitle(texts)
                SignInAction(state, language, texts, actions)
                Text(
                    text = "${texts.address}: ${state.address}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Значок кабинета, его название и объяснение под значком. */
@Composable
private fun DoorTitle(texts: CabinetTexts) {
    Icon(
        imageVector = AppIcons.cabinet,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(Sizes.headerIcon)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)
    ) {
        Text(texts.title, style = MaterialTheme.typography.headlineSmall)
        InfoTip(texts.hints.signIn)
    }
}

/**
 * Главное действие двери: вход — или ожидание подписи на его месте.
 *
 * Ожидание встаёт туда же, где стояла кнопка: владелец прерывает его тем
 * же местом, где начал. Тем же действием вход просит и мастер подключения.
 */
@Composable
internal fun SignInAction(
    state: CabinetUiState,
    language: Language,
    texts: CabinetTexts,
    actions: CabinetActions,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val since = state.signingSince
    if (since == null) {
        BusyButton(
            text = if (state.busy) texts.signing else texts.signIn,
            busy = state.busy,
            modifier = modifier,
            onClick = actions::signIn
        )
    } else {
        SignWait(
            left = leftOf(since),
            window = Signer.SIGN_WINDOW,
            texts = texts,
            eds = textsOf(language).cabinet.eds,
            onCancel = actions::cancelSignIn
        )
    }
}

/**
 * Сколько ожидания осталось.
 *
 * Отсчёт идёт от начала входа, а срок — тот же, что у подписывающего:
 * считать его здесь своим значением значит однажды показать владельцу
 * время, которого у него нет.
 */
@Composable
private fun leftOf(since: TimeMark): Duration {
    var left by remember(since) { mutableStateOf(Signer.SIGN_WINDOW) }
    LaunchedEffect(since) {
        while (isActive) {
            left = (Signer.SIGN_WINDOW - since.elapsedNow()).coerceAtLeast(Duration.ZERO)
            delay(Durations.everySecond)
        }
    }
    return left
}
