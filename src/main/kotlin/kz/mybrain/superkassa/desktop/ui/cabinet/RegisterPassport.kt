package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Паспорт кассы: то, что о ней записано в кабинете.
 *
 * Название стоит крупно и отдельно от реквизитов: прежде оно шло тем же
 * кеглем, что и пять строк под ним, и карточка кассы открывалась столбцом
 * из шести одинаковых строк без единой опоры для глаза.
 */
@Composable
fun RegisterPassport(register: CabinetRegister, texts: CabinetTexts) {
    SectionCard(
        title = texts.passport,
        trailing = { CabinetStatusChip(register.status, texts) }
    ) {
        Text(
            text = registerTitle(register),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            DetailLine(texts.registrationNumber, register.registrationNumber)
            DetailLine(texts.factoryNumber, register.factoryNumber)
            DetailLine(texts.model, register.model?.name ?: register.model?.modelCode)
            DetailLine(texts.manufactureYear, register.manufactureYear.takeIf { it > 0 }?.toString())
            DetailLine(texts.place, register.retailPlace?.name)
        }
    }
}
