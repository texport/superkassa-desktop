package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.dialog.formDialogWidth
import kz.mybrain.superkassa.designsystem.keyboard.onEnter
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.kassa.model.sale.ExciseRules
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.words.kassa.words

/**
 * Акцизные марки позиции.
 *
 * Марку кассир сканирует с бутылки или пачки — по марке на каждую единицу
 * товара. Сканер вводит код и жмёт Enter сам, поэтому поле здесь одно
 * и оно единственное действие окна: набирать марку руками не приходится.
 *
 * Марки применяются сразу, а не по кнопке: кассир сканирует их подряд
 * и должен видеть, что каждая принята. Отмены поэтому нет — есть удаление
 * ошибочной марки из перечня.
 */
@Composable
fun ExciseDialog(stamps: List<String>, onChanged: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val texts = LocalSaleTexts.current
    var scanned by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<String?>(null) }

    fun accept() {
        if (scanned.isBlank()) return
        refusal = ExciseRules.refusal(stamps, scanned)?.words(texts)
        onChanged(ExciseRules.accept(stamps, scanned))
        scanned = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.formDialogWidth(),
        icon = { Icon(AppIcons.excise, contentDescription = null) },
        title = { Text(texts.excise) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
            ) {
                ScanField(scanned, refusal ?: texts.exciseHint, refusal != null, ::accept) {
                    scanned = it
                    refusal = null
                }
                StampList(stamps) { at -> onChanged(stamps.filterIndexed { index, _ -> index != at }) }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(texts.exciseDone) } }
    )
}

/** Поле сканера: код приходит целиком, Enter приносит его в перечень. */
@Composable
private fun ScanField(
    value: String,
    hint: String,
    rejected: Boolean,
    onEnter: () -> Unit,
    onChange: (String) -> Unit
) {
    val texts = LocalSaleTexts.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(texts.excise) },
        supportingText = { Text(hint) },
        isError = rejected,
        singleLine = true,
        // Марка буквенно-цифровая: клавиатура обычная, «Готово» принимает марку, как Enter сканера.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEnter() }),
        modifier = Modifier.fillMaxWidth().onEnter {
            onEnter()
            true
        }
    )
}

/** Считанные марки: моноширинно и по одной в строке — их сверяют глазами. */
@Composable
private fun ColumnScope.StampList(stamps: List<String>, onRemove: (Int) -> Unit) {
    val texts = LocalSaleTexts.current
    if (stamps.isEmpty()) {
        Text(
            text = texts.exciseEmpty,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    // Перечень берёт остаток высоты окна и прокручивается в нём: окно
    // Material ограничено высотой экрана, и кнопка «Готово» остаётся на месте.
    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
        itemsIndexed(stamps) { at, stamp ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stamp, style = MoneyStyle.row, modifier = Modifier.weight(1f))
                // Значок называется тем, что делает: прежде он назывался
                // «Марок нет» — надписью пустого перечня, стоявшей рядом.
                IconButton(onClick = { onRemove(at) }) {
                    Icon(AppIcons.remove, contentDescription = LocalStrings.current.sale.remove)
                }
            }
        }
    }
}
