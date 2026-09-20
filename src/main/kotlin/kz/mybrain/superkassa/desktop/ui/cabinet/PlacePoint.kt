package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.map.MapPickerDialog
import kz.mybrain.superkassa.desktop.ui.map.MapPoint
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Место торговой точки — строкой и кнопкой карты.
 *
 * Полей широты и долготы в форме больше нет. В адресном регистре
 * координат нет, взять их владельцу неоткуда, и два поля с подписью
 * «Координаты точки в градусах: их вводит владелец» занимали четыре
 * строки, ничего не объясняя. Место указывается на карте, а набрать
 * градусы руками можно там же — в окне карты, где это нужно тем, у кого
 * координаты уже есть.
 *
 * @param address выбранный в регистре адрес: карта откроется на нём,
 *   и в самой карте адрес выбирается тем же регистром.
 * @param onAddress адрес, выбранный в окне карты: он и адрес формы —
 *   одна и та же запись регистра.
 */
@Composable
fun PlacePoint(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    point: MapPoint?,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onPoint: (MapPoint) -> Unit
) {
    var onMap by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = point?.let { "${texts.latitude}: ${it.latitude}${Glyphs.SEPARATOR}${texts.longitude}: ${it.longitude}" }
                ?: texts.pointNotChosen,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        FieldButton(text = texts.pickOnMap) { onMap = true }
    }
    if (onMap) {
        MapPickerDialog(
            session = session,
            cabinet = cabinet,
            texts = texts,
            latitude = point?.latitude,
            longitude = point?.longitude,
            address = address,
            onAddress = onAddress,
            onDismiss = { onMap = false }
        ) { latitude, longitude -> onPoint(MapPoint(latitude, longitude)) }
    }
}

/** Пределы широты и долготы в градусах — те же, что у кабинета. */
const val MAX_LATITUDE = "90"
const val MAX_LONGITUDE = "180"

/**
 * Градусы из набранного, если это число в допустимых пределах.
 *
 * Пределы те же, что у кабинета: он отвергает выходящее за них, и узнать
 * об этом после нажатия — значит потерять весь заполненный ввод.
 */
fun degreesOf(value: String, limit: String): BigDecimal? =
    value.trim().replace(',', '.').toBigDecimalOrNull()
        ?.takeIf { it.abs() <= BigDecimal(limit) }
