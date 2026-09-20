package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.desktop.ui.components.onEnter
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
        refusal = ExciseRules.refusal(stamps, scanned, texts)
        onChanged(ExciseRules.accept(stamps, scanned))
        scanned = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(Sizes.formDialog),
        icon = { Icon(AppIcons.excise, contentDescription = null) },
        title = { Text(texts.excise) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
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
        modifier = Modifier.fillMaxWidth().onEnter {
            onEnter()
            true
        }
    )
}

/** Считанные марки: моноширинно и по одной в строке — их сверяют глазами. */
@Composable
private fun StampList(stamps: List<String>, onRemove: (Int) -> Unit) {
    val texts = LocalSaleTexts.current
    if (stamps.isEmpty()) {
        Text(
            text = texts.exciseEmpty,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = Sizes.stampList)) {
        itemsIndexed(stamps) { at, stamp ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stamp, style = MoneyStyle.row, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(at) }) {
                    Icon(AppIcons.remove, contentDescription = texts.exciseEmpty)
                }
            }
        }
    }
}
