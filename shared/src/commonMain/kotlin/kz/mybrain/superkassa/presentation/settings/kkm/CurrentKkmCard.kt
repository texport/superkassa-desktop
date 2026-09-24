package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kkm.model.orgAddress
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Какая касса сейчас в работе.
 *
 * Карточка приподнята над остальными разделами: с неё кассир начинает
 * читать экран и по ней понимает, настройки какой машины перед ним.
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            KkmHeading(kkm.displayName, actions)
            Text(
                text = whatItIs(current, kkm.displayName, texts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NameField(kkm, actions)
            RenameActions(kkm, actions)
        }
    }
}

/** Какая касса перед кассиром и дверь к выбору другой. */
@Composable
private fun KkmHeading(name: String, actions: KkmSettingsActions) {
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = texts.settings.currentKkm,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(name, style = MaterialTheme.typography.titleLarge)
        }
        TextButton(onClick = actions::switchKkm) { Text(texts.settings.changeKkm) }
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
    val texts = LocalStrings.current.settings
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
    val money = moneyTexts(LocalLanguage.current).kkm
    WrapRow {
        FilledTonalButton(enabled = !kkm.busy, onClick = actions::saveName) { Text(texts.settings.save) }
        OutlinedButton(enabled = kkm.nameField.isNotBlank() && !kkm.busy, onClick = actions::resetName) {
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
private fun whatItIs(kkm: KkmResponse, name: String, texts: AppStrings): String = listOfNotNull(
    kkm.orgTitle ?: texts.settings.orgUnknown,
    kkm.factoryNumber?.let { "${texts.login.factory} $it" },
    kkm.orgAddress
).joinToString(Glyphs.SEPARATOR).ifBlank { name }
