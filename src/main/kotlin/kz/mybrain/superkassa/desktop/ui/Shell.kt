package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetScreen
import kz.mybrain.superkassa.desktop.ui.cabinet.ownerLine
import kz.mybrain.superkassa.desktop.ui.cash.CashScreen
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.KkmStatusChips
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.ReceiptPreview
import kz.mybrain.superkassa.desktop.ui.dashboard.DashboardScreen
import kz.mybrain.superkassa.desktop.ui.history.HistoryScreen
import kz.mybrain.superkassa.desktop.ui.login.LoginScreen
import kz.mybrain.superkassa.desktop.ui.queue.QueueScreen
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UsersScreen

/**
 * Каркас окна.
 *
 * Разделы — рельсом слева, как в настольном Material 3; над содержимым —
 * шапка приложения с кассой, её состоянием и действиями. Состояние узла
 * и кассы висит всегда и на всех разделах: кассир должен увидеть разрыв
 * связи до того, как пробьёт чек, а не после.
 */
@Composable
fun Shell(session: Session) {
    var section by remember { mutableStateOf(Section.Dashboard) }
    // Кабинет живёт рядом с кассовым сеансом, а не внутри него: вход туда
    // свой — по ЭЦП владельца, — и переживает переходы между разделами.
    val cabinet = remember { CabinetSession(CabinetClient(session.preferences.cabinetUrl)) }
    // Один хост сообщений на окно: снекбар лежит поверх содержимого
    // и не сдвигает разметку под руками кассира.
    val messages = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val texts = LocalStrings.current

    LaunchedEffect(Unit) {
        session.refreshKkms()
        session.loadDictionaries()
    }

    // Вошли — сразу подтягиваем состояние выбранной кассы.
    LaunchedEffect(session.selected?.kkmId, session.pin) {
        if (session.signedIn) {
            session.refreshSelected()
        }
    }

    if (!session.signedIn) {
        // До входа кассир видит только вход: пустые разделы без выбранной
        // кассы отвечают отказами и ничему не учат.
        Scaffold(
            topBar = { BusyLine(session.busy) },
            snackbarHost = { MessageHost(messages) }
        ) { padding ->
            MessageEffect(session.lastMessage, messages) { session.lastMessage = null }
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LoginScreen(session, cabinet)
            }
        }
        return
    }

    // Кассиру видны только его разделы: очередь, кассиры и настройки узел
    // отдаёт администратору, и пустой отказ вместо экрана ничему не учит.
    val sections = Section.entries.filter { session.isAdmin || !it.adminOnly }
    if (section !in sections) {
        section = Section.Dashboard
    }

    // Шапка идёт во всю ширину окна, а рельс разделов — под ней: иначе
    // шапка начиналась правее рельса и накрывала его край, а окно
    // выглядело собранным из двух несогласованных половин.
    Scaffold(
        topBar = {
            Column {
                // Шапка называет то, чем владелец сейчас распоряжается:
                // в кабинете это компания и он сам, в остальных разделах —
                // касса и кассир. Раздел один, и шапка одна.
                if (section == Section.Cabinet && cabinet.open) {
                    CabinetTopBar(session, cabinet)
                } else {
                    KkmTopBar(
                        session = session,
                        onSignOut = { session.signOut() },
                        onRefresh = {
                            scope.launch {
                                session.refreshKkms()
                                session.refreshSelected()
                            }
                        }
                    )
                }
                BusyLine(session.busy)
            }
        },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        MessageEffect(session.lastMessage, messages) { session.lastMessage = null }
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Ширина рельса считается по самой длинной подписи набора:
            // у Material она фиксированная, и «Новая касса» упиралась
            // в край окна. Считается так же, как ширина сегментов, —
            // одним правилом на весь интерфейс.
            val measurer = rememberTextMeasurer()
            val labelStyle = MaterialTheme.typography.labelMedium
            val widest = sections.maxOfOrNull {
                measurer.measure(it.title(texts.sections), labelStyle).size.width
            } ?: 0
            val railWidth = with(LocalDensity.current) { widest.toDp() } + Spacing.roomy * 2
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(if (session.railCollapsed) Sizes.rail else maxOf(railWidth, Sizes.rail)),
                header = {
                    // Свёрнутый рельс отдаёт ширину чеку: значки кассир знает
                    // наизусть, а подписи нужны первую неделю.
                    IconButton(onClick = { session.toggleRail() }) {
                        Icon(
                            imageVector = AppIcons.menu,
                            contentDescription = if (session.railCollapsed) {
                                texts.common.expand
                            } else {
                                texts.common.collapse
                            }
                        )
                    }
                }
            ) {
                sections.forEach { entry ->
                    NavigationRailItem(
                        selected = section == entry,
                        onClick = { section = entry },
                        icon = { Icon(entry.icon, contentDescription = entry.title(texts.sections)) },
                        label = if (session.railCollapsed) {
                            null
                        } else {
                            { Text(entry.title(texts.sections)) }
                        }
                    )
                }
            }
            // Своего отступа у каркаса нет: поля разделов уже задают его
            // одним значением, и второй отступ поверх делал колонку
            // содержимого шире положенного.
            Box(modifier = Modifier.fillMaxSize()) {
                when (section) {
                    Section.Dashboard -> DashboardScreen(session)
                    Section.Sale -> SaleScreen(session)
                    Section.Returns -> ReturnsScreen(session)
                    Section.Cash -> CashScreen(session)
                    Section.History -> HistoryScreen(session)
                    Section.Queue -> QueueScreen(session)
                    Section.Users -> UsersScreen(session)
                    Section.Register -> ConnectKkmScreen(session, cabinet)
                    Section.Cabinet -> CabinetScreen(session, cabinet)
                    Section.Settings -> SettingsScreen(session)
                }
                // Печатная форма живёт над всеми разделами: кассир открывает
                // её из журнала и вправе уйти в продажу, не теряя окна.
                ReceiptPreview(
                    image = session.preview,
                    onPrint = { session.printDesk.printShown() },
                    onSave = { session.printDesk.saveShown() }
                ) { session.preview = null }
            }
        }
    }
}

/**
 * Шапка приложения: какая касса и в каком она состоянии.
 *
 * Заголовок — номер кассы, подзаголовок — организация и смена. Плашки
 * состояния стоят до действий: кассир читает слева направо и должен
 * узнать о блокировке раньше, чем дотянется до кнопки.
 */
@Composable
private fun KkmTopBar(session: Session, onSignOut: () -> Unit, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    val kkm = session.selected
    AppTopBar(
        title = kkm?.let { session.displayName(it) } ?: texts.shell.noKkm,
        subtitle = kkm?.let { listOfNotNull(it.orgTitle, session.whoami?.name).joinToString(SEPARATOR) }
    ) {
        KkmStatusChips(session)
        IconButton(onClick = onRefresh) {
            Icon(AppIcons.refresh, contentDescription = texts.common.refresh)
        }
        LanguagePicker(session)
        TextButton(onClick = onSignOut) { Text(texts.shell.changeCashier) }
    }
}

/**
 * Шапка кабинета: кто вошёл и чем он распоряжается.
 *
 * Язык и выход стоят там же, где у кассы, и теми же элементами: владелец,
 * перешедший из кассовой части в кабинет, не должен искать их заново.
 * Язык переключается здесь же — кабинет государственный, и владелец вправе
 * вести его по-казахски, не выходя обратно на экран входа.
 */
@Composable
private fun CabinetTopBar(session: Session, cabinet: CabinetSession) {
    val texts = cabinetTexts(session.language)
    val scope = rememberCoroutineScope()
    AppTopBar(
        title = cabinet.company?.name.orEmpty(),
        subtitle = ownerLine(cabinet, texts),
        badge = AppIcons.cabinet
    ) {
        LanguagePicker(session)
        TextButton(onClick = { scope.launch { cabinet.signOut() } }) { Text(texts.signOut) }
    }
}

/** Разделитель между сведениями в подзаголовке. */
private const val SEPARATOR = " · "

/**
 * Полоска ожидания под шапкой.
 *
 * Один индикатор на всё окно, а не свой у каждой кнопки: обращение к узлу
 * идёт из любого раздела, и кассир должен видеть, что касса занята,
 * не гадая, какая кнопка сейчас работает. Место постоянное — полоска
 * не сдвигает содержимое, когда появляется.
 */
@Composable
private fun BusyLine(busy: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(Sizes.busyLine)) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
