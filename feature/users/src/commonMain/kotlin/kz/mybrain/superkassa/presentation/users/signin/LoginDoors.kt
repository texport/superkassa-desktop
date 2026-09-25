package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Списка касс нет.
 *
 * Пустой список и непрочитанный список — разные беды, и делать вид, что
 * они одна, нельзя: пока список не прочитан, «на этом узле ни одной кассы»
 * утверждает то, чего приложение не знает, а «Новая касса» главным
 * действием посылает кассира заводить кассу там, где не читается даже
 * список.
 *
 * Кабинет и настройки здесь кнопками не повторяются: они — соседние
 * разделы навигации окна и видны всегда.
 *
 * @param listRead прочитан ли список касс; `false` — касса промолчала
 *   или ответила отказом, и о кассах неизвестно ничего.
 * @param onRegister открыть раздел «Новая касса»; `null` — мастера нет.
 */
@Composable
internal fun EmptyKkms(listRead: Boolean, actions: LoginActions, onRegister: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        if (listRead) {
            NoKkms(onRegister)
        } else {
            KkmsUnread(actions)
        }
    }
}

/** Узел ответил, и касс на нём правда нет: первую заводит владелец. */
@Composable
private fun NoKkms(onRegister: (() -> Unit)?) {
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
    if (onRegister != null) Button(onClick = onRegister) { Text(texts.sections.register) }
}

/**
 * Список не прочитан: о кассах неизвестно ничего.
 *
 * Главное действие здесь — повтор, а не заведение кассы: пока список
 * не читается, заводить некуда. Сам отказ кассир читает строкой
 * сообщения внизу окна.
 */
@Composable
private fun KkmsUnread(actions: LoginActions) {
    val texts = LocalStrings.current
    EmptyState(
        icon = AppIcons.warning,
        title = texts.login.kkmsUnreadTitle,
        hint = texts.login.kkmsUnread
    )
    Button(onClick = actions::reload) { Text(texts.login.reload) }
}
