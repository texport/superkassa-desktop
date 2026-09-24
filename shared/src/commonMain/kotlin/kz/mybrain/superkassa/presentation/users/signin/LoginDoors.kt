package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Заголовок экрана: название и то, что рядом с ним ставит каркас окна —
 * вид, язык, состояние связи.
 */
@Composable
internal fun LoginHeader(extras: @Composable RowScope.() -> Unit) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            texts.login.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f)
        )
        extras()
    }
}

/**
 * Списка касс нет.
 *
 * Пустой список и непрочитанный список — разные беды, и делать вид, что
 * они одна, нельзя: пока список не прочитан, «на этом узле ни одной кассы»
 * утверждает то, чего приложение не знает, а «Новая касса» главным
 * действием посылает кассира заводить кассу там, где не читается даже
 * список.
 *
 * @param listRead прочитан ли список касс; `false` — касса промолчала
 *   или ответила отказом, и о кассах неизвестно ничего.
 */
@Composable
internal fun EmptyKkms(listRead: Boolean, actions: LoginActions, doors: Set<Door>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        if (listRead) {
            NoKkms(actions, doors)
        } else {
            KkmsUnread(actions, doors)
        }
    }
}

/** Узел ответил, и касс на нём правда нет: первую заводит владелец. */
@Composable
private fun NoKkms(actions: LoginActions, doors: Set<Door>) {
    val texts = LocalStrings.current
    EmptyState(
        icon = AppIcons.kkm,
        // Название о том, чего нет, а не о невыбранной кассе: выбирать
        // здесь не из чего, и «Касса не выбрана» над объяснением
        // «не заведено ни одной кассы» называло другую беду.
        title = texts.login.noKkmsTitle,
        hint = texts.login.noKkms
    )
    // Завести кассу — главное действие пустого экрана: список прочитан,
    // и касса появится в нём, как только её заведут.
    if (Door.Register in doors) Button(onClick = { actions.open(Door.Register) }) { Text(texts.sections.register) }
    // Вторая дверь стоит и здесь: у владельца, который только начал,
    // нет ни кассы, ни компании, ни точки — и всё это заводится
    // в кабинете, а не в кассе.
    DoorButtons(doors, listOf(Door.Cabinet, Door.Settings), actions)
}

/**
 * Список не прочитан: о кассах неизвестно ничего.
 *
 * Главное действие здесь — повтор, а не заведение кассы: пока список
 * не читается, заводить некуда. Сам отказ кассир читает строкой
 * сообщения внизу окна.
 */
@Composable
private fun KkmsUnread(actions: LoginActions, doors: Set<Door>) {
    val texts = LocalStrings.current
    EmptyState(
        icon = AppIcons.warning,
        title = texts.login.kkmsUnreadTitle,
        hint = texts.login.kkmsUnread
    )
    Button(onClick = actions::reload) { Text(texts.login.reload) }
    // Кабинет живёт своей службой и отвечает, когда касса молчит: владельцу
    // остаётся хотя бы он.
    DoorButtons(doors, listOf(Door.Settings, Door.Cabinet), actions)
}

/**
 * Двери [order] из тех, что есть на платформе, — в порядке [order].
 *
 * @param doors двери, за которыми на этой платформе что-то есть.
 */
@Composable
internal fun DoorButtons(doors: Set<Door>, order: List<Door>, actions: LoginActions) {
    val sections = LocalStrings.current.sections
    order.filter { it in doors }.forEach { door ->
        when (door) {
            Door.Register -> DoorButton(AppIcons.newKkm, sections.register) { actions.open(door) }
            Door.Cabinet -> DoorButton(AppIcons.cabinet, sections.cabinet) { actions.open(door) }
            Door.Settings -> DoorButton(AppIcons.settings, sections.settings) { actions.open(door) }
            Door.Kkms -> Unit
        }
    }
}

/**
 * Дверь с экрана входа: заведение кассы и кабинет ОФД.
 *
 * Рамка и значок, а не текст вподбор: это входы для владельца, и они
 * должны читаться как входы, не соперничая при этом с главным действием
 * экрана — входом кассира по пину.
 */
@Composable
internal fun DoorButton(icon: ImageVector, title: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
        Text(text = title, modifier = Modifier.padding(start = Spacing.itemGap))
    }
}
