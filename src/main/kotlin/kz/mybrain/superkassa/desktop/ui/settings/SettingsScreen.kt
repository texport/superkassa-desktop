package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Настройки — один экран на всё приложение.
 *
 * Открывается из двух мест: с экрана входа, где кассы ещё нет, и из кассы,
 * куда кассир вошёл. Прежде это были два разных экрана со своими списками
 * карточек, и они разошлись: отладка стояла только за входом — то есть
 * ровно там, где она уже не нужна, потому что войти получилось.
 *
 * Список карточек один, а показывается каждая по своим условиям:
 * настройке кассы нужна выбранная касса, служебной — права
 * администратора. До входа ни того, ни другого нет, и остаётся то, что
 * задают раньше, чем куда-либо войти.
 */
@Composable
fun SettingsScreen(session: Session) {
    val texts = LocalStrings.current.settings
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = texts.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.screen)
        )
        SettingsCards(session, Modifier.weight(1f))
    }
}

/**
 * Карточки настроек, разложенные по хозяйствам.
 *
 * Вкладка первого уровня называет владельца настройки — рабочее место,
 * касса, кабинет БФД, — а заголовок внутри неё называет предмет. Вкладки,
 * а не второй список слева: слева уже стоит колонка разделов кассы,
 * и вторая вертикаль рядом с ней читалась бы как её продолжение.
 *
 * Вкладка стоит над прокруткой и не уезжает вместе с содержимым: переход
 * между хозяйствами не должен требовать возврата наверх.
 *
 * Вынесено отдельно от экрана: тот же набор стоит в окне настроек
 * рабочего места, где у него своя шапка с возвратом.
 */
@Composable
fun SettingsCards(session: Session, modifier: Modifier = Modifier) {
    val texts = LocalStrings.current.settings
    val shown = settingsCards.filter { it.visible(session.selected != null, session.isAdmin) }
    val households = SettingsHousehold.entries.filter { household ->
        shown.any { it.group.household == household }
    }
    // Владелец с выбранной кассой приходит в настройки к ней: открывать
    // ему вид приложения значит заставлять нажимать вкладку каждый раз.
    var wanted by remember(session.selected != null) {
        mutableStateOf(if (session.selected == null) SettingsHousehold.Workplace else SettingsHousehold.Kkm)
    }
    val chosen = wanted.takeIf { it in households } ?: households.first()
    Column(modifier = modifier) {
        if (households.size > 1) {
            HouseholdTabs(households, chosen) { wanted = it }
        }
        ScrollableColumn(
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            spacing = Spacing.roomy
        ) {
            SettingsGroup.entries.filter { it.household == chosen }.forEach { group ->
                val cards = shown.filter { it.group == group }
                if (cards.isEmpty()) return@forEach
                group.title(texts)?.let { GroupTitle(it) }
                cards.forEach { it.card(session) }
            }
        }
    }
}

/** Вкладки хозяйств: только те, в которых сейчас что-то есть. */
@Composable
private fun HouseholdTabs(
    households: List<SettingsHousehold>,
    chosen: SettingsHousehold,
    onChoose: (SettingsHousehold) -> Unit
) {
    val texts = LocalStrings.current.settings
    PrimaryTabRow(selectedTabIndex = households.indexOf(chosen)) {
        households.forEach { household ->
            Tab(
                selected = household == chosen,
                onClick = { onChoose(household) },
                text = { Text(household.title(texts)) }
            )
        }
    }
}

/**
 * Заголовок раздела внутри хозяйства.
 *
 * Карточек в настройках десяток, и без разделов они читаются одним
 * списком: кассир ищет нужную глазами по всему экрану.
 */
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
