package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.cabinet.SignInAction
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.ConfirmDangerDialog
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
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
    var way by remember { mutableStateOf(SetupWay.ViaCabinet) }
    var startingOver by remember { mutableStateOf(false) }

    if (startingOver) {
        ConfirmDangerDialog(
            what = setup.startOverAsk,
            explain = setup.startOverExplain,
            action = setup.startOver,
            cancel = moneyTexts(session.language).drawer.cancel,
            onCancel = { startingOver = false },
            onConfirm = {
                startingOver = false
                draft.clear()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Возврат и название стоят в шапке, а не в прокручиваемом
        // содержимом: мастер длинный, и уехавшая вверх стрелка означала,
        // что выйти из него можно, только домотав до конца.
        AppTopBar(
            title = setup.title,
            subtitle = setup.explain,
            onBack = onBack,
            backLabel = texts.settings.back
        ) {
            // Пока мастер ничего не прошёл, забывать нечего, и кнопка
            // не стоит: нажатая по ошибке, она стирает заводской номер,
            // уже унесённый в кабинет.
            if (draft.factoryNumber != null) {
                TextButton(onClick = { startingOver = true }) { Text(setup.startOver) }
            }
        }
        ScrollableColumn(
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            spacing = Spacing.snug
        ) {
            ChoiceSegments(
                options = SetupWay.entries,
                selected = way,
                label = { it.title(setup) },
                onSelect = { way = it }
            )
            when (way) {
                SetupWay.ViaCabinet -> {
                    // Вход нужен всем шагам, кроме первого, и жить он обязан
                    // здесь: спрятанный внутри пройденного шага, он исчезал
                    // вместе с ним — мастер, продолженный назавтра, упирался
                    // в «ждёт предыдущего шага» без единой кнопки.
                    //
                    // Но только там, где своей кнопки нет: пока касса
                    // в кабинете не заведена, вход предлагает сам шаг —
                    // под объяснением, зачем он. Двумя одинаковыми синими
                    // кнопками подряд экран спрашивал одно и то же дважды.
                    if (!cabinet.open && draft.cabinetRegisterId != null) {
                        // Та же кнопка, что на двери кабинета: со сроком
                        // ожидания и отменой. Своя занятая кнопка молчала
                        // о том, сколько ждать и чем прервать.
                        SignInAction(
                            cabinet = cabinet,
                            language = session.language,
                            texts = cabinetTexts(session.language),
                            modifier = Modifier
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
