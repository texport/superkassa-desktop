package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Адрес личного кабинета ОФД.
 *
 * Кабинет — отдельная служба: на этой машине он занимает соседний порт,
 * на стенде стоит своим адресом. Новый адрес берётся при следующем входе
 * в раздел кабинета, а не на лету: менять адрес под открытым сеансом
 * значит показывать чужие данные под прежним именем.
 */
@Composable
fun CabinetAddressCard(workplace: WorkplaceSettingsUiState, actions: WorkplaceSettingsActions) {
    val texts = cabinetTexts(LocalLanguage.current)
    val settings = LocalStrings.current.settings
    SectionCard(title = texts.address, info = texts.hints.address) {
        // Адрес занимает остаток строки карточки, кнопка стоит за ним:
        // адрес службы длиннее любой заданной ширины поля. Негодный адрес
        // назван до сохранения: по адресу без схемы входа в кабинет не будет
        // вовсе, а на экране это выходило как «кабинет не отвечает».
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = workplace.cabinetField,
                onValueChange = actions::typeCabinet,
                label = { Text(texts.address) },
                isError = workplace.cabinetMalformed,
                supportingText = if (workplace.cabinetMalformed) ({ Text(settings.addressMalformed) }) else null,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FieldButton(text = texts.save, enabled = workplace.cabinetChanged, onClick = actions::saveCabinet)
        }
    }
}
