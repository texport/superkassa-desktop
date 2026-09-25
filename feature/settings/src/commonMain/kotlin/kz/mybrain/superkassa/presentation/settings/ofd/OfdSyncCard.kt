package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Сверка кассы с БФД.
 *
 * Всё, что касса знает о себе, приходит от БФД: организация, адрес,
 * регистрационные номера, счётчики и номер смены. Разойтись они могут
 * после автономной работы или замены сведений в кабинете БФД — тогда
 * кассир сверяет их отсюда, а не переустанавливает кассу.
 *
 * Аббревиатура расшифрована в подсказке у заголовка — один раз
 * на приложение, а не в каждой надписи, где БФД упомянута.
 *
 * Условие стоит под своей кнопкой, а не общим списком внизу: у сверки
 * сведений и сверки счётчиков требования разные, и общий список заставлял
 * бы вспоминать, какое из них к чему.
 *
 * Кнопка гаснет там, где касса заведомо откажет: пока в очереди лежат
 * неотправленные документы и — у сверки сведений — пока смена открыта. Условие написано под значком у той же кнопки,
 * и отказ после нажатия не сообщал кассиру ничего нового.
 */
@Composable
internal fun OfdSyncCard(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    SettingGroup(title = money.syncTitle, info = money.bfdMeaning) {
        SyncAction(money.syncService, money.syncServiceHint, ofd.serviceSyncable, actions::syncService)
        SyncAction(money.syncCounters, money.syncCountersHint, ofd.syncable, actions::syncCounters)
    }
}

/**
 * Кнопка сверки и условие, при котором касса её выполнит.
 *
 * Условие — под значком: строкой во всю ширину оно занимало у каждой
 * кнопки по две строки экрана, а читают его один раз.
 */
@Composable
private fun SyncAction(title: String, hint: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(title) }
        InfoTip(hint)
    }
}
