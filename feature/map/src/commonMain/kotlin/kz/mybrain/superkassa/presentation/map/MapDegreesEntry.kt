package kz.mybrain.superkassa.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.button.FieldButton
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.mapview.HOUSE_ZOOM
import kz.mybrain.superkassa.presentation.common.mapview.MAX_LATITUDE
import kz.mybrain.superkassa.presentation.common.mapview.MAX_LONGITUDE
import kz.mybrain.superkassa.presentation.common.mapview.MapState
import kz.mybrain.superkassa.presentation.common.mapview.degrees
import kz.mybrain.superkassa.presentation.common.mapview.degreesOf
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Ввод градусов руками.
 *
 * Место карты — на карте, но у кого координаты уже есть — из замера или
 * из чужой карты, — тому проще их вписать. Поле живёт здесь, а не в форме
 * точки: в форме два поля градусов с подписями занимали четыре строки
 * и стояли перед владельцем всегда, а нужны они изредка.
 */
@Composable
internal fun DegreesEntry(state: MapState, texts: CabinetTexts) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DegreeField(texts.places.latitude, latitude, MAX_LATITUDE, Modifier.weight(1f)) { latitude = it }
        DegreeField(texts.places.longitude, longitude, MAX_LONGITUDE, Modifier.weight(1f)) { longitude = it }
        // Пара собирается целиком или не собирается вовсе: половина точки
        // на карту не ставится, и проверять её потом второй раз незачем.
        val point = degreesOf(latitude, MAX_LATITUDE)?.let { north ->
            degreesOf(longitude, MAX_LONGITUDE)?.let { east -> north to east }
        }
        FieldButton(text = texts.map.showDegrees, enabled = point != null) {
            point?.let { (north, east) -> state.show(north.degrees(), east.degrees(), HOUSE_ZOOM) }
        }
    }
}

/** Поле градусов: отмечает недопустимое значение до нажатия. */
@Composable
private fun DegreeField(label: String, value: String, limit: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = value.isNotBlank() && degreesOf(value, limit) == null,
        singleLine = true,
        modifier = modifier
    )
}
