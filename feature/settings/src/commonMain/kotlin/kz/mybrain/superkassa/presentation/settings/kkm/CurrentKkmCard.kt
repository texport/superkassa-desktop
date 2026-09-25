package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.kkm.model.orgAddress
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Какая касса сейчас в работе.
 *
 * С неё начинается раздел кассы: по названию кассир понимает, настройки
 * какой машины перед ним. Подзаголовок группы — «Текущая касса», рядом —
 * дверь к выбору другой, под ним — название крупно и чья это касса.
 *
 * Название кассы уходит в кассу: так её зовут на входе, до того как
 * кассир набрал пин.
 * Прочие сведения приходят от ОФД и здесь не меняются. Переименование
 * нужно там, где в зале три одинаковых кассы и их различают по месту,
 * а не по номеру.
 *
 * Смена кассы уводит на экран входа, а не переключается на месте: иначе
 * кассир меняет кассу, не заметив, и пробивает чек не на той машине.
 */
@Composable
internal fun CurrentKkmCard(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val texts = LocalStrings.current
    val current = kkm.kkm ?: return
    SettingGroup(
        title = texts.settingsScreen.currentKkm,
        trailing = { TextButton(onClick = actions::switchKkm) { Text(texts.settingsScreen.changeKkm) } }
    ) {
        Text(kkm.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = whatItIs(current, kkm.displayName, texts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NameField(kkm, actions)
        RenameActions(kkm, actions)
    }
}

/**
 * Название кассы.
 *
 * Поле во всю ширину карточки, кнопки под ним: название кассы бывает
 * на сотню знаков, и в поле заданной ширины от него оставалось начало.
 * Строкой «поле — две кнопки» ряд на крупной ступени по-казахски
 * не помещался и переносил кнопки вразнобой.
 */
@Composable
private fun NameField(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val texts = LocalStrings.current.settingsScreen
    OutlinedTextField(
        value = kkm.nameField,
        onValueChange = actions::typeName,
        label = { Text(texts.localName) },
        trailingIcon = { InfoTip(texts.localNameHint) },
        singleLine = true,
        enabled = !kkm.busy,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Сохранение названия и возврат к названию от ОФД.
 *
 * Итог правки объявляет только удачу: отказ кассы уже показан её же
 * словами. Кнопки гаснут на время обращения: второе нажатие отправляло
 * бы то же название второй раз.
 */
@Composable
private fun RenameActions(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val texts = LocalStrings.current
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    // Сохранение названия — главное действие раздела кассы: оно одно
    // залито цветом, возврат к названию от ОФД — текстовой кнопкой рядом.
    WrapRow {
        Button(enabled = !kkm.busy, onClick = actions::saveName) { Text(texts.settingsScreen.save) }
        TextButton(enabled = kkm.nameField.isNotBlank() && !kkm.busy, onClick = actions::resetName) {
            Text(money.renameReset)
        }
    }
}

/**
 * Организация, заводской номер и адрес — одной строкой под названием.
 *
 * Организация здесь названа всегда: карточка отвечает на вопрос, чья это
 * касса, и пропуск строки читался бы как её отсутствие в карточке, а не
 * как отсутствие сведений у ОФД.
 */
private fun whatItIs(kkm: KkmResponse, name: String, texts: CommonTexts): String = listOfNotNull(
    kkm.orgTitle ?: texts.settingsScreen.orgUnknown,
    kkm.factoryNumber?.let { "${texts.login.factory} $it" },
    kkm.orgAddress
).joinToString(Glyphs.SEPARATOR).ifBlank { name }
