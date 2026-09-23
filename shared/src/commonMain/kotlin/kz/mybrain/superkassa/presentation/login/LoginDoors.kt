package kz.mybrain.superkassa.presentation.login

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
import kz.mybrain.superkassa.presentation.components.EmptyState
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Заголовок экрана: название и то, что рядом с ним ставит каркас окна —
 * вид, язык, состояние связи.
 */
@Composable
internal fun LoginHeader(extras: @Composable RowScope.() -> Unit) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
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
internal fun EmptyKkms(listRead: Boolean, actions: LoginActions) {
    val onCabinet = { actions.open(Door.Cabinet) }
    val onSettings = { actions.open(Door.Settings) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        if (listRead) {
            NoKkms({ actions.open(Door.Register) }, onCabinet, onSettings)
        } else {
            KkmsUnread(actions::reload, onSettings, onCabinet)
        }
    }
}

/** Узел ответил, и касс на нём правда нет: первую заводит владелец. */
@Composable
private fun NoKkms(onRegister: () -> Unit, onCabinet: () -> Unit, onSettings: () -> Unit) {
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
    Button(onClick = onRegister) { Text(texts.sections.register) }
    // Вторая дверь стоит и здесь: у владельца, который только начал,
    // нет ни кассы, ни компании, ни точки — и всё это заводится
    // в кабинете, а не в кассе.
    DoorButton(AppIcons.cabinet, texts.sections.cabinet, onCabinet)
    DoorButton(AppIcons.settings, texts.sections.settings, onSettings)
}

/**
 * Список не прочитан: о кассах неизвестно ничего.
 *
 * Главное действие здесь — повтор, а не заведение кассы: пока список
 * не читается, заводить некуда. Сам отказ кассир читает строкой
 * сообщения внизу окна.
 */
@Composable
private fun KkmsUnread(onReload: () -> Unit, onSettings: () -> Unit, onCabinet: () -> Unit) {
    val texts = LocalStrings.current
    EmptyState(
        icon = AppIcons.warning,
        title = texts.login.kkmsUnreadTitle,
        hint = texts.login.kkmsUnread
    )
    Button(onClick = onReload) { Text(texts.login.reload) }
    DoorButton(AppIcons.settings, texts.sections.settings, onSettings)
    // Кабинет живёт своей службой и отвечает, когда касса молчит: владельцу
    // остаётся хотя бы он.
    DoorButton(AppIcons.cabinet, texts.sections.cabinet, onCabinet)
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
        Text(text = title, modifier = Modifier.padding(start = Spacing.tight))
    }
}
