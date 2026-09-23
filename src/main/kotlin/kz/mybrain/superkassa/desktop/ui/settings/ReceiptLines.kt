package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.Branding
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings

/**
 * Свои строки кассы на чеке.
 *
 * Рекламу оператора присылает ОФД, а это — тексты самой торговой точки:
 * приветствие в шапке, условия возврата под позициями, благодарность
 * в подвале. Места печати заданы протоколом печатной формы, поэтому
 * поле названо тем местом, где строка окажется.
 *
 * Набранное переживает уход в другой раздел: экран настроек уходит
 * из состава вместе с ним, и девять полей, набранных для точки,
 * пропадали от одного взгляда в журнал. Черновик у каждой кассы свой.
 */
@Composable
fun ReceiptLinesSection(kkmId: String, branding: Branding, enabled: Boolean, onSave: (Branding) -> Unit) {
    val texts = LocalStrings.current
    // Нетронутое поле оставляется как есть: узел отдаёт ненабранную
    // строку пустотой, и подстановка пустой строки поверх неё зажигала бы
    // «Сохранить» на кассе, у которой владелец ничего не менял.
    val edited = ReceiptLine.entries.fold(branding) { carried, line ->
        SettingsDrafts.draft(line.field(kkmId))?.let { line.write(carried, it) } ?: carried
    }

    PartTitle(texts.settings.receiptLines, texts.settings.receiptLinesHint)
    // Поля идут столбцом в том порядке, в каком строки встанут на чеке,
    // и во всю ширину карточки: строка на сто знаков видна целиком
    // и переносится внутри поля. Сеткой по два-три в ряд порядок читался
    // слева направо и обрывался на краю, а однострочное поле показывало
    // от строки первые двадцать знаков.
    ReceiptLine.entries.forEach { line ->
        OutlinedTextField(
            value = line.read(edited).orEmpty(),
            onValueChange = { SettingsDrafts.type(line.field(kkmId), it) },
            label = { Text(line.title(texts.settings)) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().keysOfSingleLine()
        )
    }
    FilledTonalButton(
        enabled = enabled && edited != branding,
        onClick = { onSave(edited) }
    ) { Text(texts.settings.saveReceiptLines) }
}

/**
 * Места печати своих строк на чеке.
 *
 * Перечисление, а не девять одинаковых полей подряд: набор задан печатной
 * формой, и добавление места должно быть строкой здесь, а не копией
 * пятнадцати строк разметки.
 */
internal enum class ReceiptLine(
    val title: (SettingStrings) -> String,
    val read: (Branding) -> String?,
    val write: (Branding, String) -> Branding
) {
    BeforeHeader({ it.lineBeforeHeader }, { it.beforeHeaderMsg }, { b, v -> b.copy(beforeHeaderMsg = v) }),
    Header({ it.lineHeader }, { it.headerMsg }, { b, v -> b.copy(headerMsg = v) }),
    AfterHeader({ it.lineAfterHeader }, { it.afterHeaderMsg }, { b, v -> b.copy(afterHeaderMsg = v) }),
    BeforeItems({ it.lineBeforeItems }, { it.beforeItemsMsg }, { b, v -> b.copy(beforeItemsMsg = v) }),
    AfterItems({ it.lineAfterItems }, { it.afterItemsMsg }, { b, v -> b.copy(afterItemsMsg = v) }),
    BeforeTotals({ it.lineBeforeTotals }, { it.beforeTotalsMsg }, { b, v -> b.copy(beforeTotalsMsg = v) }),
    AfterTotals({ it.lineAfterTotals }, { it.afterTotalsMsg }, { b, v -> b.copy(afterTotalsMsg = v) }),
    BeforeQr({ it.lineBeforeQr }, { it.beforeQrMsg }, { b, v -> b.copy(beforeQrMsg = v) }),
    Footer({ it.lineFooter }, { it.footerMsg }, { b, v -> b.copy(footerMsg = v) });

    /** Имя черновика этой строки у этой кассы. */
    fun field(kkmId: String): String =
        SettingsDrafts.forKkm("${SettingsDrafts.Field.RECEIPT_LINE}-$name", kkmId)
}

/** Узел строки принял — черновики больше не нужны. */
internal fun forgetReceiptLineDrafts(kkmId: String) {
    ReceiptLine.entries.forEach { SettingsDrafts.forget(it.field(kkmId)) }
}
