package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.enroll.AddRegisterDialog
import kz.mybrain.superkassa.presentation.cabinet.places.AddPlaceCard
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Создание кассы и точки — окнами.
 *
 * Заливкой набрано одно: владелец приходит сюда заводить кассу, а точку
 * создаёт постольку, поскольку кассе нужен адрес. Две тональные кнопки
 * подряд обещали два равных дела и заставляли читать обе, чтобы выбрать.
 *
 * Точка — тональной кнопкой, а не текстовой. Пока точка не выбрана,
 * касса недоступна, и единственным действием колонки оставалась строка
 * текста под погашенной заливкой: её не находили. Тональная кнопка
 * по Material 3 — второе по важности действие, и она видна рядом
 * с погашенной главной.
 *
 * Кнопки стоят рядом и переносятся целиком: столбиком по всей ширине
 * они отнимали у списка точек ещё одну строку.
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
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: String?
) {
    var addingPlace by remember { mutableStateOf(false) }
    var addingRegister by remember { mutableStateOf(false) }
    WrapRow(modifier = Modifier.fillMaxWidth().padding(end = Spacing.fieldGap)) {
        Button(enabled = place != null, onClick = { addingRegister = true }) { Text(texts.addRegister) }
        FilledTonalButton(onClick = { addingPlace = true }) { Text(texts.addPlace) }
    }
    if (addingPlace) {
        AddPlaceCard(cabinet, texts, onDismiss = { addingPlace = false }, onAdded = {})
    }
    if (addingRegister) {
        AddRegisterDialog(cabinet, texts, onDismiss = { addingRegister = false }) {}
    }
}
