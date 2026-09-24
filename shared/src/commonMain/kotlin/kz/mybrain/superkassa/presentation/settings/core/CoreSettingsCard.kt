package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.section.FactLines
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts
import kz.mybrain.superkassa.presentation.theme.StatusColors

/**
 * Касса на этой машине: как она работает и сколько ждёт БФД.
 *
 * Сведения — режим, протокол, хранилище — первое, что спрашивает
 * поддержка при разборе, и читаются они сами. Сроки обмена с БФД владелец
 * меняет здесь; закрытая правка видна плашкой до нажатия, а поля гаснут.
 */
@Composable
internal fun CoreSettingsCard(core: CoreSettingsUiState, actions: CoreSettingsActions) {
    val language = LocalLanguage.current
    val texts = coreSettingTexts(language)
    val money = moneyTexts(language).kkm
    SectionCard(
        title = texts.title,
        info = texts.hint,
        trailing = { if (core.frozen) Chip(texts.frozen, StatusColors.pending) }
    ) {
        FactLines(null, core.facts(texts, money), texts.unread)
        if (core.settings == null) return@SectionCard
        Note(texts.protocolFixed)
        if (core.frozen) Note(if (core.server) texts.serverHint else texts.frozenHint)
        SecondsField(money.bfdTimeout, core.timeout, core.timeoutValid, !core.frozen, actions::typeTimeout)
        SecondsField(texts.reconnect, core.reconnect, core.reconnectValid, !core.frozen, actions::typeReconnect)
        WrapRow {
            FilledTonalButton(enabled = core.savable, onClick = actions::save) {
                Text(LocalStrings.current.settings.save)
            }
        }
    }
}

/** Поле секунд: негодное названо под полем до сохранения. */
@Composable
private fun SecondsField(label: String, value: String, valid: Boolean, enabled: Boolean, onChange: (String) -> Unit) {
    val texts = coreSettingTexts(LocalLanguage.current)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = !valid,
        supportingText = if (valid) null else ({ Text(texts.seconds) }),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Пояснение под сведениями и над полями: его читают один раз и не нажимают. */
@Composable
internal fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
