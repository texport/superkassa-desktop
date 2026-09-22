package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Окно подробностей строки чека.
 *
 * Строка корзины вмещает наименование, количество и сумму; коды, марки
 * и наименование на казахском в неё не влезают, а кассир, которого
 * спросили о них у прилавка, обязан ответить, не открывая справочник.
 * Нажатие на строку открывает это окно.
 *
 * Действия со строкой — сторно и удаление — те же, что и на самой
 * карточке, и делают то же самое: окно их не повторяет своей логикой,
 * а вызывает обработчики корзины. У строки чека-основания действий нет:
 * пробитый чек уже не правят.
 *
 * Надписи, единицы и ставки приходят снаружи, а не берутся из контекста
 * экрана продажи: окно открывают и на возврате, где этот контекст
 * не собран.
 */
@Composable
fun PositionDetailsDialog(
    details: PositionDetails,
    texts: SaleTexts,
    units: List<UnitOfMeasurement>,
    rates: List<VatRate>,
    onDismiss: () -> Unit,
    onStorno: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val labels = LocalStrings.current.sale
    val rows = details.rows(labels, texts, units, rates)
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(Sizes.formDialog),
        icon = { Icon(AppIcons.receiptLine, contentDescription = null) },
        title = { Text(texts.positionDetails) },
        text = { DetailRows(rows) },
        dismissButton = { LineActions(details.storno, onStorno, onRemove) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(texts.positionClose) } }
    )
}

/**
 * Строки «подпись — значение».
 *
 * Список ленивый и ограничен по высоте: у позиции с марками и кодами
 * строк больше десяти, и без предела окно уходило бы за нижний край
 * вместе с кнопкой закрытия.
 */
@Composable
private fun DetailRows(rows: List<Pair<String, String>>) {
    ScrollableList(modifier = Modifier.fillMaxWidth().heightIn(max = Sizes.detailList)) {
        items(rows) { (label, value) ->
            ListItem(
                overlineContent = { Text(label) },
                headlineContent = { Text(value) },
                // Подложка окна и строки — одна поверхность: отдельная
                // подложка у каждой строки рисовала бы полосы поверх окна.
                colors = ListItemDefaults.colors(containerColor = AlertDialogDefaults.containerColor)
            )
        }
    }
}

/**
 * Действия со строкой корзины.
 *
 * Сторно — обратимая отметка, пока чек не пробит, поэтому подпись
 * говорит, что сделает нажатие сейчас: поставит отметку или снимет.
 */
@Composable
private fun LineActions(storno: Boolean, onStorno: (() -> Unit)?, onRemove: (() -> Unit)?) {
    val labels = LocalStrings.current.sale
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        if (onStorno != null) {
            TextButton(onClick = onStorno) {
                Text(if (storno) labels.stornoUndo else labels.storno)
            }
        }
        if (onRemove != null) {
            TextButton(onClick = onRemove) {
                Text(labels.remove, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
