package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDoor
import kz.mybrain.superkassa.desktop.ui.components.LoadingState
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules

/**
 * Вход в кассу.
 *
 * Кассир выбирает свою кассу и вводит пин один раз за смену — дальше пин
 * не спрашивается ни на одном экране. Касса запоминается: на рабочем месте
 * она не меняется, и утром достаточно ввести пин.
 *
 * Раскладка та же, что у списка с действием по Material 3: перечень
 * прокручивается, а пин и кнопка стоят в отдельной поверхности внизу
 * и никуда не уезжают, сколько бы касс ни было в списке.
 */
@Composable
fun LoginScreen(session: Session, cabinet: CabinetSession) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Kkm?>(null) }
    var search by remember { mutableStateOf("") }
    // Заведение кассы открывается прямо отсюда: пока не заведена первая
    // касса, войти некуда, а настройки живут за входом.
    var registering by remember { mutableStateOf(false) }
    // Кабинет открывается до выбора кассы и без пина: пока первая касса
    // не заведена, пина кассира не существует вовсе, а завести кассу
    // можно только начав с кабинета.
    var atCabinet by remember { mutableStateOf(false) }
    // Настройки рабочего места открываются до входа: адрес узла и адрес
    // кабинета нужны раньше, чем есть куда войти.
    var atSettings by remember { mutableStateOf(false) }

    // Узел отвечает не мгновенно, и до его ответа список пуст: без этого
    // кассир на запуске читал «касс нет» про узел с десятком касс.
    var answered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        session.refreshKkms()
        answered = true
    }

    // Подстановка считается тут же, а не в отложенном эффекте: список
    // приходит с узла позже первой отрисовки, и эффект успевал отработать
    // на пустом списке — кассир видел «касса не выбрана» при запомненной.
    val shown = session.kkms.filter { it.matches(search, session.displayName(it)) }

    // Когда поиск оставил ровно одну кассу, она и есть выбранная: кассир
    // набирает номер своей кассы и сразу пин, не тянясь к мыши.
    // Порядок важен: набранный кассиром номер сильнее всего остального.
    // Иначе после смены кассира поиск другой кассы ничего не менял —
    // подставлялась прежняя, и кассир входил не туда, куда набрал.
    val chosenKkm = chosen
        ?: shown.singleOrNull()
        ?: session.kkms.firstOrNull { it.kkmId == session.rememberedKkmId }
        ?: session.selected

    /**
     * Вход: касса и пин запоминаются в сеансе.
     *
     * Перечитывание состояния делает каркас — он живёт всё время работы,
     * а этот экран исчезает в тот же миг, и запущенное здесь обновление
     * обрывалось бы на полпути.
     */
    fun enter() {
        val kkm = chosenKkm ?: return
        scope.launch { session.signIn(kkm, pin) }
    }

    val reload = { scope.launch { session.refreshKkms() } }
    if (registering) {
        ConnectKkmScreen(session, cabinet) { registering = false }
        return
    }
    if (atCabinet) {
        CabinetDoor(session, cabinet) { atCabinet = false }
        return
    }
    if (atSettings) {
        WorkplaceSettingsScreen(session) { atSettings = false }
        return
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Поля и шаг между блоками задаёт колонка, а не каждый блок сам:
        // когда шапка, список, двери и пин несли по своему отступу, все
        // четыре зазора выходили разными.
        Column(
            modifier = Modifier.width(Sizes.loginColumn).fillMaxSize().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            LoginHeader(session)
            if (session.kkms.isEmpty() && !answered) {
                LoadingState(Modifier.weight(1f))
                return@Column
            }
            if (session.kkms.isEmpty()) {
                EmptyKkms(
                    onReload = { reload() },
                    onCabinet = { atCabinet = true },
                    onRegister = { registering = true },
                    onSettings = { atSettings = true }
                )
                return@Column
            }
            // Набранный номер отменяет прежний выбор мышью: он сделан позже,
            // а значит и означает намерение кассира. Иначе набор «2000042»
            // после клика по другой кассе не менял ничего, и кассир входил
            // не туда, куда набрал.
            SearchField(
                value = search,
                label = texts.login.search,
                icon = AppIcons.kkm,
                onChange = {
                    search = it
                    chosen = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            KkmList(
                kkms = shown,
                nameOf = { session.displayName(it) },
                chosenId = chosenKkm?.kkmId,
                rememberedId = session.rememberedKkmId,
                modifier = Modifier.weight(1f)
            ) { chosen = it }
            // Две двери рядом: кассир входит пином ниже, владелец —
            // своей ЭЦП в кабинет. Обе со значками и в рамке: текстовыми
            // вподбор они терялись, а кабинет для нового владельца —
            // единственный вход, пока нет ни кассы, ни компании.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                DoorButton(AppIcons.newKkm, texts.sections.register) { registering = true }
                DoorButton(AppIcons.cabinet, texts.sections.cabinet) { atCabinet = true }
                DoorButton(AppIcons.settings, texts.sections.settings) { atSettings = true }
            }
            SignInBar(
                pin = pin,
                nameOf = { session.displayName(it) },
                onPin = { pin = UserRules.digitsOf(it) },
                chosen = chosenKkm,
                onEnter = ::enter,
                onReload = { reload() }
            )
        }
    }
}
