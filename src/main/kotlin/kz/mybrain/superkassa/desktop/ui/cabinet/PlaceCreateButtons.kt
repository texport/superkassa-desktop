package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Создание кассы и точки — окнами.
 *
 * Заливкой набрано одно: владелец приходит сюда заводить кассу, а точку
 * создаёт постольку, поскольку кассе нужен адрес. Две тональные кнопки
 * подряд обещали два равных дела и заставляли читать обе, чтобы выбрать.
 *
 * Кнопки отступают от правого края на то же поле, что и список над ними:
 * иначе кнопка шире списка ровно на ширину полосы прокрутки, и края
 * не сходятся.
 *
 * Живут отдельно от самой колонки: колонке для показа хватает готовых
 * строк, а окна заведения работают с кабинетом и рабочим местом.
 */
@Composable
internal fun PlaceCreateButtons(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: String?,
    onChanged: () -> Unit
) {
    var addingPlace by remember { mutableStateOf(false) }
    var addingRegister by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Button(
            enabled = place != null,
            onClick = { addingRegister = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(texts.addRegister) }
        TextButton(onClick = { addingPlace = true }, modifier = Modifier.fillMaxWidth()) {
            Text(texts.addPlace)
        }
    }
    if (addingPlace) {
        AddPlaceCard(session, cabinet, texts, onDismiss = { addingPlace = false }, onAdded = onChanged)
    }
    if (addingRegister) {
        AddRegisterDialog(session, cabinet, texts, onDismiss = { addingRegister = false }) { onChanged() }
    }
}
