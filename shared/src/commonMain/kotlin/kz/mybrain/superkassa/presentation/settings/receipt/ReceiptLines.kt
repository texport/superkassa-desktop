package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.section.PartTitle
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.settings.keysOfSingleLine
import kz.mybrain.superkassa.presentation.settings.title

/**
 * Свои строки кассы на чеке.
 *
 * Рекламу оператора присылает ОФД, а это — тексты самой торговой точки:
 * приветствие в шапке, условия возврата под позициями, благодарность
 * в подвале. Места печати заданы протоколом печатной формы, поэтому
 * поле названо тем местом, где строка окажется.
 *
 * Поля идут столбцом в том порядке, в каком строки встанут на чеке,
 * и во всю ширину карточки: строка на сто знаков видна целиком
 * и переносится внутри поля. Сеткой по два-три в ряд порядок читался
 * слева направо и обрывался на краю.
 */
@Composable
fun ReceiptLinesSection(form: ReceiptFormUiState, actions: ReceiptFormActions) {
    val texts = LocalStrings.current
    val edited = form.edited
    PartTitle(texts.settings.receiptLines, texts.settings.receiptLinesHint)
    ReceiptLine.entries.forEach { line ->
        OutlinedTextField(
            value = line.read(edited).orEmpty(),
            onValueChange = { actions.typeLine(line, it) },
            label = { Text(line.title(texts.settings)) },
            enabled = form.editable,
            modifier = Modifier.fillMaxWidth().keysOfSingleLine()
        )
    }
    FilledTonalButton(
        enabled = form.editable && !form.busy && edited != form.branding,
        onClick = actions::saveLines
    ) { Text(texts.settings.saveReceiptLines) }
}
