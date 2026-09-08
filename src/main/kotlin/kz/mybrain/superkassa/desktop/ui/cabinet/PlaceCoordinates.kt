package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.map.MapPickerDialog
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Координаты торговой точки: карта, ввод и разбор.
 *
 * Одни и те же на заведение точки и на её переезд. В переезде проверки
 * не было вовсе: непрочитанное значение молча уходило нулём — то есть
 * в Гвинейский залив вместо Алматы.
 *
 * Место указывается на карте: в адресном регистре координат нет, и
 * владелец брал их неизвестно откуда. Поля градусов остались рядом
 * с картой, а не вместо неё: карта требует сети, а касса стоит и там,
 * где сети нет, — и тогда координаты приходят из другого источника
 * и вводятся руками.
 */
@Composable
fun PlaceCoordinates(
    texts: CabinetTexts,
    latitude: String,
    longitude: String,
    onLatitude: (String) -> Unit,
    onLongitude: (String) -> Unit
) {
    var onMap by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.Top
    ) {
        DegreeField(texts.latitude, latitude, MAX_LATITUDE, texts.required, onLatitude)
        DegreeField(texts.longitude, longitude, MAX_LONGITUDE, texts.coordinatesHint, onLongitude)
        FieldButton(text = texts.pickOnMap) { onMap = true }
    }
    if (onMap) {
        MapPickerDialog(
            texts = texts,
            latitude = degreesOf(latitude, MAX_LATITUDE),
            longitude = degreesOf(longitude, MAX_LONGITUDE),
            onDismiss = { onMap = false }
        ) { chosenLatitude, chosenLongitude ->
            onLatitude(chosenLatitude.toPlainString())
            onLongitude(chosenLongitude.toPlainString())
        }
    }
}

/** Поле градусов: отмечает недопустимое значение до нажатия кнопки. */
@Composable
private fun DegreeField(
    label: String,
    value: String,
    limit: String,
    hint: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(hint) },
        isError = value.isNotBlank() && degreesOf(value, limit) == null,
        singleLine = true,
        modifier = Modifier.width(Sizes.fieldChoice)
    )
}

/** Верны ли обе координаты — то есть примет ли их кабинет. */
fun coordinatesReady(latitude: String, longitude: String): Boolean =
    degreesOf(latitude, MAX_LATITUDE) != null && degreesOf(longitude, MAX_LONGITUDE) != null

/**
 * Градусы из набранного, если это число в допустимых пределах.
 *
 * Пределы те же, что у кабинета: он отвергает выходящее за них, и узнать
 * об этом после нажатия — значит потерять весь заполненный ввод.
 */
fun degreesOf(value: String, limit: String): BigDecimal? =
    value.trim().replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it.abs() <= BigDecimal(limit) }

/** Пределы широты и долготы в градусах. */
const val MAX_LATITUDE = "90"
const val MAX_LONGITUDE = "180"
