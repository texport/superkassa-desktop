package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.desktop.app.PinRequest
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Печатная форма поверх всего, что открыто.
 *
 * Стоит над обоими входами в приложение: над разделами кассы и над
 * дверью в кабинет с экрана входа. Прежде окно просмотра жило только
 * за входом кассира, и владелец, пришедший в кабинет прямо с экрана
 * входа, нажимал «показать» — а показывать форму было негде.
 *
 * Рядом с ним живёт запрос пина: форму рисует касса, узел пускает
 * к ней по пину, а кассир мог и не входить.
 */
@Composable
fun PrintOverlay(session: Session) {
    ReceiptPreview(
        image = session.preview,
        drawing = session.drawing,
        onPrint = { session.printDesk.printShown() },
        onSave = { session.printDesk.saveShown() }
    ) {
        session.printDesk.closePreview()
    }
    session.printDesk.drawer.request?.let { request ->
        DrawPinDialog(request, onDismiss = { session.printDesk.drawer.cancel() }) {
            session.printDesk.drawer.adopt(it)
        }
    }
}

/**
 * Пин кассы ради печатной формы.
 *
 * Спрашивается отдельно от входа кассира и входом не считается: введённый
 * здесь пин живёт только в памяти и разделы кассы не открывает. Название
 * кассы стоит первой строкой — пин у касс разный, и владелец должен
 * видеть, к какой именно его спрашивают.
 */
@Composable
private fun DrawPinDialog(request: PinRequest, onDismiss: () -> Unit, onEnter: (String) -> Unit) {
    val texts = LocalStrings.current
    var pin by remember(request) { mutableStateOf("") }
    FormDialog(
        title = texts.preview.drawPin,
        icon = AppIcons.pin,
        action = texts.preview.draw,
        close = texts.preview.close,
        busy = false,
        missing = if (pin.isBlank()) listOf(texts.common.pin) else emptyList(),
        onDismiss = onDismiss,
        onAction = { onEnter(pin) }
    ) {
        Text(text = request.kkmTitle, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(texts.common.pin) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.width(Sizes.fieldPin)
        )
        Text(
            text = texts.preview.drawPinHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
