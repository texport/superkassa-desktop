package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.strings.api.settings.DeliverySettingTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Доставка чека покупателю: включённые каналы, адреса служб и ключи.
 *
 * Получателя у канала нет: чек уходит на контакт покупателя, который
 * кассир указал в самом чеке. Без контакта чек покупателю не уходит.
 *
 * Каждый канал — строкой-переключателем с пометкой «настроен» и полями под ней:
 * владелец видит, уйдёт ли чек, до первого чека, а не по жалобе. Закрытая
 * правка видна плашкой до нажатия, поля гаснут, под ними — почему.
 */
@Composable
fun DeliveryCard(delivery: DeliveryUiState, actions: DeliveryActions) {
    val texts = textsOf(LocalLanguage.current).settings.delivery
    val core = textsOf(LocalLanguage.current).settings.core
    SettingGroup(
        title = texts.title,
        info = texts.hint,
        trailing = { if (delivery.frozen) Chip(core.frozen, StatusColors.pending) }
    ) {
        if (!delivery.read) return@SettingGroup
        Note(texts.recipient)
        if (delivery.frozen) Note(if (delivery.server) core.serverHint else core.frozenHint)
        if (!delivery.frozen && DeliveryField.entries.any(delivery::hidden)) Note(texts.secretHint)
        DeliveryChannel.entries.forEach { ChannelFields(it, delivery, actions, texts) }
        WrapRow {
            Button(enabled = delivery.savable, onClick = actions::save) {
                Text(LocalStrings.current.settingsScreen.save)
            }
        }
    }
}

/** Канал: название, настроен ли он, включён ли, и его поля. */
@Composable
private fun ChannelFields(
    channel: DeliveryChannel,
    delivery: DeliveryUiState,
    actions: DeliveryActions,
    texts: DeliverySettingTexts
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        ChannelHead(channel, delivery, actions, texts)
        WrapRow {
            DeliveryField.of(channel).forEach { field -> DeliveryInput(field, delivery, actions, texts) }
        }
    }
}

/**
 * Строка канала — переключатель Material 3: название, под ним — настроен
 * ли канал, в конце — шлёт ли он каждый чек. Владелец видит, уйдёт ли
 * чек, до первого чека, а не по жалобе.
 */
@Composable
private fun ChannelHead(
    channel: DeliveryChannel,
    delivery: DeliveryUiState,
    actions: DeliveryActions,
    texts: DeliverySettingTexts
) {
    SwitchRow(
        title = texts.channels.of(channel),
        checked = delivery.enabled(channel),
        onSwitch = { actions.switch(channel, it) },
        enabled = !delivery.frozen && !delivery.busy,
        hint = if (channel in delivery.configured) texts.configured else texts.notConfigured
    )
}

/**
 * Поле канала. Заданный ключ показывается знаком, набираемый — точками:
 * экран настроек показывают и снимают. Негодное названо под полем.
 */
@Composable
private fun FlowRowScope.DeliveryInput(
    field: DeliveryField,
    delivery: DeliveryUiState,
    actions: DeliveryActions,
    texts: DeliverySettingTexts
) {
    val malformed = delivery.malformed(field)
    val dotted = field.secret && !delivery.hidden(field)
    OutlinedTextField(
        value = delivery.value(field),
        onValueChange = { actions.type(field, it) },
        label = { Text(texts.fields.of(field)) },
        isError = malformed,
        supportingText = if (malformed) ({ Text(texts.problem(field)) }) else null,
        singleLine = true,
        enabled = !delivery.frozen && !delivery.busy,
        visualTransformation = if (dotted) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = field.keyboard),
        modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldForm)
    )
}

/** Клавиатура поля: ключ — паролем, порт — цифрами, адреса — адресом. */
private val DeliveryField.keyboard: KeyboardType
    get() = when {
        secret -> KeyboardType.Password
        this == DeliveryField.EmailPort -> KeyboardType.Number
        this == DeliveryField.EmailFrom -> KeyboardType.Email
        this == DeliveryField.SmsUrl -> KeyboardType.Uri
        else -> KeyboardType.Text
    }

/** Что не так с негодным полем: у порта — диапазон, у адресов — написание. */
private fun DeliverySettingTexts.problem(field: DeliveryField): String =
    if (field == DeliveryField.EmailPort) portRange else malformed
