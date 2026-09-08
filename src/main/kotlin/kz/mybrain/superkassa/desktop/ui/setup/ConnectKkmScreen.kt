package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetIssue
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Подключение кассы: от заводского номера до входа кассира.
 *
 * Кассу нельзя завести ни с одного конца отдельно. Узел заводит её по
 * идентификатору и токену, которые выдаёт ОФД; ОФД заводит её по заводскому
 * номеру, который выдаёт узел; а войти в приложение можно только пином
 * кассира, которого до заведения кассы не существует. Поэтому мастер идёт
 * через обе службы подряд и переносит числа между ними сам — прежде их
 * носили буфером обмена, и достаточно было открыть экран дважды, чтобы
 * унести в кабинет один номер, а завести кассу с другим.
 *
 * Мастер переживает закрытие приложения: заявление в ИСНА рассматривают
 * не в ту же минуту, а ключ ЭЦП бывает у владельца, который придёт завтра.
 * Пройденное лежит в [KkmSetupDraft], токен туда не попадает.
 *
 * @param onBack возврат туда, откуда пришли; `null` — возвращаться некуда.
 */
@Composable
fun ConnectKkmScreen(
    session: Session,
    cabinet: CabinetSession,
    onBack: (() -> Unit)? = null
) {
    val texts = LocalStrings.current
    val setup = setupTexts(session.language)
    val draft = remember { KkmSetupDraft(session.preferences) }
    val scope = rememberCoroutineScope()
    var way by remember { mutableStateOf(SetupWay.ViaCabinet) }

    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.snug
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.back, contentDescription = texts.settings.back)
                }
            }
            Text(setup.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { draft.clear() }) { Text(setup.startOver) }
        }
        Text(
            text = setup.explain,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChoiceSegments(
            options = SetupWay.entries,
            selected = way,
            label = { it.title(setup) },
            onSelect = { way = it }
        )
        when (way) {
            SetupWay.ViaCabinet -> {
                CabinetIssue(cabinet, cabinetTexts(session.language))
                // Вход нужен всем шагам, кроме первого, и жить он обязан
                // здесь: спрятанный внутри пройденного шага, он исчезал
                // вместе с ним — мастер, продолженный назавтра, упирался
                // в «ждёт предыдущего шага» без единой кнопки.
                if (!cabinet.open) {
                    val cabinetTexts = cabinetTexts(session.language)
                    BusyButton(
                        text = if (cabinet.busy) cabinetTexts.signing else cabinetTexts.signIn,
                        busy = cabinet.busy,
                        onClick = { scope.launch { cabinet.signIn() } }
                    )
                }
                FactoryStepCard(session, setup, draft)
                CabinetStepCard(session, cabinet, setup, draft)
                var registered by remember(draft.cabinetRegisterId) { mutableStateOf(false) }
                ApplicationStepCard(session, cabinet, setup, draft) { registered = true }
                AdminStepCard(session, cabinet, setup, draft, registered) { onBack?.invoke() }
            }
            SetupWay.ByHand -> {
                Text(
                    text = setup.manuallyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Заводской номер тот же и запоминается так же: свой шаг
                // для ручного пути выдал бы владельцу второй номер,
                // отличный от унесённого в кабинет.
                FactoryStepCard(session, setup, draft)
                // Второй шаг в такой же карточке, как первый: раньше он
                // рисовал свой заголовок снаружи, и два шага выглядели
                // как куски из разных экранов.
                // Второй шаг доступен сразу: в ручном пути кассу в ОФД
                // завели без владельца, и заводской номер отсюда ему
                // не нужен — идентификатор и токен уже на руках.
                SetupStepCard(
                    title = setup.stepAdmin,
                    hint = setup.stepAdminHint,
                    texts = setup,
                    done = false,
                    ready = true
                ) {
                    OfdStep(session)
                }
            }
        }
    }
}

/**
 * Каким путём подключают кассу.
 *
 * Через кабинет — когда владелец с ключом ЭЦП сидит за этим же
 * компьютером. Вручную — когда кассу в ОФД завёл сервисник или бухгалтер,
 * и на руках только идентификатор и токен: заставлять в этом случае
 * проходить кабинет значит требовать чужой ключ.
 */
enum class SetupWay(val title: (SetupTexts) -> String) {
    ViaCabinet({ it.viaCabinet }),
    ByHand({ it.manually })
}
