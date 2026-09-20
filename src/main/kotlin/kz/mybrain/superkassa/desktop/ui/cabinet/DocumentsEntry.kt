package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Переход к документам кассы из её карточки.
 *
 * Прежде документы стояли списком внутри карточки кассы: под паспортом,
 * состоянием и заявлениями, в полосе шириной в половину экрана и с высотой
 * в четыре строки. Ни поиска, ни отбора, ни печати в такой полосе
 * не помещалось, а сам список читался как добавка к карточке, а не как
 * журнал кассы. Теперь в карточке остаётся переход, а журнал открывается
 * экраном во всю ширину рабочего места.
 */
@Composable
fun RegisterDocumentsCard(language: Language, register: CabinetRegister) {
    val journal = journalTexts(language).history
    val open = LocalRegisterDocuments.current
    SectionCard(title = journal.registerDocuments, info = journal.registerDocumentsHint) {
        FilledTonalButton(onClick = { open(register) }) {
            Icon(AppIcons.history, contentDescription = null)
            Text(journal.openDocuments, modifier = Modifier.padding(start = Spacing.tight))
        }
    }
}
