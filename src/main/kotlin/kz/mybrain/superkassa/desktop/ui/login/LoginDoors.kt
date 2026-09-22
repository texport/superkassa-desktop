package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.ThemeSwitch
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/** Заголовок экрана: название, язык и состояние связи с узлом. */
@Composable
internal fun LoginHeader(session: Session) {
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
        ThemeSwitch(session)
        LanguagePicker(session)
        Chip(
            text = if (session.nodeAvailable) texts.common.nodeOnline else texts.common.nodeOffline,
            color = if (session.nodeAvailable) StatusColors.delivered else StatusColors.refused
        )
    }
}

/**
 * Списка касс нет.
 *
 * Пустой список и молчание узла — разные беды, и делать вид, что они одна,
 * нельзя: на молчащем узле «на этом узле ни одной кассы» утверждает то,
 * чего приложение не знает, а «Новая касса» главным действием посылает
 * кассира заводить кассу там, где не читается даже список.
 *
 * @param nodeAnswered ответил ли узел по существу; `false` — не ответил
 *   вовсе, и о кассах на нём неизвестно ничего.
 */
@Composable
internal fun EmptyKkms(
    nodeAnswered: Boolean,
    onReload: () -> Unit,
    onCabinet: () -> Unit,
    onRegister: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        if (nodeAnswered) {
            NoKkms(onRegister, onCabinet, onSettings)
        } else {
            NodeSilent(onReload, onSettings, onCabinet)
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
        // «узел не отдал ни одной кассы» называло другую беду.
        title = texts.login.noKkmsTitle,
        hint = texts.login.noKkms
    )
    // Завести кассу — главное действие пустого экрана: узел на связи,
    // и ему есть чем ответить, как только касса появится.
    Button(onClick = onRegister) { Text(texts.sections.register) }
    // Вторая дверь стоит и здесь: у владельца, который только начал,
    // нет ни кассы, ни компании, ни точки — и всё это заводится
    // в кабинете, а не в кассе.
    DoorButton(AppIcons.cabinet, texts.sections.cabinet, onCabinet)
    DoorButton(AppIcons.settings, texts.sections.settings, onSettings)
}

/**
 * Узел не ответил: о кассах на нём неизвестно ничего.
 *
 * Главное действие здесь — повтор, а не заведение кассы: пока список
 * не читается, заводить на этом узле нечего. Вторым идут настройки —
 * чаще всего молчит не узел, а неверно записанный его адрес.
 */
@Composable
private fun NodeSilent(onReload: () -> Unit, onSettings: () -> Unit, onCabinet: () -> Unit) {
    val texts = LocalStrings.current
    EmptyState(
        icon = AppIcons.warning,
        title = texts.login.nodeSilentTitle,
        hint = texts.login.nodeSilent
    )
    Button(onClick = onReload) { Text(texts.login.reload) }
    DoorButton(AppIcons.settings, texts.sections.settings, onSettings)
    // Кабинет живёт своей службой и отвечает, когда узел молчит: владельцу
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
