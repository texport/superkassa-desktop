package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetScreen
import kz.mybrain.superkassa.desktop.ui.cash.CashScreen
import kz.mybrain.superkassa.desktop.ui.components.PrintOverlay
import kz.mybrain.superkassa.desktop.ui.dashboard.DashboardScreen
import kz.mybrain.superkassa.desktop.ui.history.HistoryScreen
import kz.mybrain.superkassa.desktop.ui.login.LoginScreen
import kz.mybrain.superkassa.desktop.ui.queue.QueueScreen
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UsersScreen

/**
 * Рельс разделов.
 *
 * Ширина считается по самой длинной подписи набора: у Material она
 * постоянная, и «Новая касса» упиралась в край окна. Считается так же,
 * как ширина сегментов, — одним правилом на весь интерфейс.
 *
 * Разделы прокручиваются, а версия под ними стоит на месте: администратору
 * их десяток, а окно кассы бывает ростом в 700 точек — на ноутбуке и на
 * экране прилавка. Без прокрутки «Настройки» уходили под нижний край
 * вместе с версией, и открыть их было нечем.
 *
 * @param footer то, что стоит в нижнем углу рельса под разделами:
 *   версия кассы и знак о новой.
 */
@Composable
internal fun SectionRail(
    sections: List<Section>,
    current: Section,
    collapsed: Boolean,
    onToggle: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    onPick: (Section) -> Unit
) {
    val texts = LocalStrings.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    // Подписи меряются один раз на набор и язык, а не на каждую перерисовку:
    // при растягивании окна разметка пересчитывается десятки раз в секунду,
    // и раскладка шрифта на каждый такой проход — работа впустую.
    val titles = sections.map { it.title(texts.sections) }
    val railWidth = remember(titles, labelStyle, density) {
        val widest = titles.maxOfOrNull { measurer.measure(it, labelStyle).size.width } ?: 0
        with(density) { widest.toDp() } + Spacing.roomy * 2
    }
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(if (collapsed) Sizes.rail else maxOf(railWidth, Sizes.rail)),
        header = { RailToggle(collapsed, onToggle) }
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            sections.forEach { entry ->
                NavigationRailItem(
                    selected = current == entry,
                    onClick = { onPick(entry) },
                    icon = { Icon(entry.icon, contentDescription = entry.title(texts.sections)) },
                    label = if (collapsed) null else ({ Text(entry.title(texts.sections)) })
                )
            }
        }
        footer()
    }
}

/**
 * Кнопка сворачивания рельса.
 *
 * Свёрнутый рельс отдаёт ширину чеку: значки кассир знает наизусть,
 * а подписи нужны первую неделю.
 */
@Composable
private fun RailToggle(collapsed: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = AppIcons.menu,
            contentDescription = if (collapsed) texts.common.expand else texts.common.collapse
        )
    }
}

/**
 * Содержимое выбранного раздела.
 *
 * Своего отступа у каркаса нет: поля разделов уже задают его одним
 * значением, и второй отступ поверх делал колонку содержимого шире
 * положенного.
 */
@Composable
internal fun SectionContent(
    session: Session,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
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
            Section.Cabinet -> CabinetScreen(session, cabinet, documents)
            Section.Settings -> SettingsScreen(session)
        }
        // Печатная форма живёт над всеми разделами: кассир открывает её
        // из журнала и вправе уйти в продажу, не теряя окна. То же окно
        // стоит и над дверью в кабинет — оно одно на оба входа.
        PrintOverlay(session)
    }
}

/** Вход кассира занимает окно целиком: пустые разделы без кассы ничему не учат. */
@Composable
internal fun SectionDoor(session: Session, cabinet: CabinetSession) {
    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreen(session, cabinet)
        // Форму владелец может открыть и отсюда — через дверь в кабинет.
        PrintOverlay(session)
    }
}
