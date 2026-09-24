package kz.mybrain.superkassa.presentation.print.preview.component

import androidx.compose.foundation.layout.fillMaxWidth
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
import kz.mybrain.superkassa.designsystem.dialog.FormDialog
import kz.mybrain.superkassa.designsystem.keyboard.onEnter
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.signin.model.Pin
import kz.mybrain.superkassa.presentation.print.preview.PaperActions
import kz.mybrain.superkassa.presentation.print.preview.PrintUiState

/**
 * Печатная форма поверх всего, что открыто.
 *
 * Стоит над обоими входами в приложение: над разделами кассы и над
 * дверью в кабинет с экрана входа. Прежде окно просмотра жило только
 * за входом кассира, и владелец, пришедший в кабинет прямо с экрана
 * входа, нажимал «показать» — а показывать форму было негде.
 *
 * Рядом с ним живёт запрос пина: форму рисует касса, и пускает она
 * к ней по пину, а кассир мог и не входить.
 */
@Composable
fun PrintOverlay(paper: PrintUiState, actions: PaperActions) {
    val texts = LocalStrings.current.preview
    ReceiptPreview(
        image = paper.image,
        drawing = paper.drawing,
        trouble = paper.trouble?.let { ScreenState.Trouble(texts.missing, it.words, actions::retry) },
        onPrint = actions::printShown,
        onSave = actions::saveShown,
        onDismiss = actions::close
    )
    paper.pinFor?.let { kkmTitle ->
        DrawPinDialog(kkmTitle, onDismiss = actions::cancelPin, onEnter = actions::enterPin)
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
private fun DrawPinDialog(kkmTitle: String, onDismiss: () -> Unit, onEnter: (String) -> Unit) {
    val texts = LocalStrings.current
    var pin by remember(kkmTitle) { mutableStateOf("") }
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
        Text(text = kkmTitle, style = MaterialTheme.typography.titleMedium)
        PinField(pin, texts.common.pin) { pin = Pin.digitsOf(it) }
        Text(
            text = texts.preview.drawPinHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Поле пина: цифры под точками, клавиатура цифровая. */
@Composable
private fun PinField(pin: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = pin,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
