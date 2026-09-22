package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.PrintPreferences
import kz.mybrain.superkassa.desktop.app.Printing
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.PrintKind
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.DictionaryPicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Куда печатает эта касса.
 *
 * Принтер выбирается один раз и держится за кассой: за одним компьютером
 * их бывает две, и чековая лента у каждой своя. Пока принтер не выбран,
 * задание уходит на системный по умолчанию — так печатает любая программа,
 * и объяснять кассиру тут нечего.
 *
 * Вид файла — то, чем сохраняют форму: PDF уходит покупателю, HTML —
 * в бухгалтерию, картинка повторяет экран.
 *
 * @param printers принтеры этой машины. Задаются снаружи только проверкой:
 *   спросить их у машины, на которой идёт проверка, значит увидеть её
 *   принтеры, а не пустой список, ради которого карточку и смотрят.
 */
@Composable
internal fun PrintTargetCard(session: Session, printers: List<String> = remember { Printing.printers() }) {
    val texts = LocalStrings.current
    val kkm = session.selected ?: return
    var printer by remember(kkm.kkmId) { mutableStateOf(session.preferences.printer(kkm.kkmId)) }
    var kind by remember { mutableStateOf(session.preferences.printKind()) }
    var copies by remember { mutableStateOf(session.preferences.printCopies) }

    SectionCard(title = texts.settings.printer, info = texts.settings.printerHint) {
        // Принтеров на машине может не быть вовсе — за прилавком это
        // обычное дело до подключения чекового. Молчание здесь кончалось
        // отказом печати на первом же чеке, при покупателе.
        if (printers.isEmpty()) {
            Text(
                text = texts.settings.printerNone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        DictionaryPicker(
            label = texts.settings.printer,
            entries = printerEntries(printers, texts.settings.printerSystem),
            language = session.language.code,
            selectedCode = printer ?: SYSTEM_PRINTER,
            onSelect = { chosen ->
                printer = chosen.takeIf { it != SYSTEM_PRINTER }
                session.preferences.choosePrinter(kkm.kkmId, printer)
            },
            width = Sizes.fieldBarcode
        )
        Text(texts.settings.printCopies, style = MaterialTheme.typography.bodyMedium)
        ChoiceSegments(
            options = (1..PrintPreferences.MAX_COPIES).toList(),
            selected = copies,
            label = { it.toString() }
        ) { chosen ->
            copies = chosen
            session.preferences.printCopies = chosen
        }
        Text(texts.settings.printKind, style = MaterialTheme.typography.bodyMedium)
        ChoiceSegments(
            options = PrintKind.entries,
            selected = kind,
            label = { it.name.uppercase() }
        ) { chosen ->
            kind = chosen
            session.preferences.choosePrintKind(chosen)
        }
    }
}

/** Список принтеров с системным первым: он же и подставляется по умолчанию. */
private fun printerEntries(printers: List<String>, systemTitle: String): List<DictionaryEntry> =
    listOf(DictionaryEntry(code = SYSTEM_PRINTER, name = mapOf("ru" to systemTitle, "kk" to systemTitle, "en" to systemTitle))) +
        printers.map { DictionaryEntry(code = it) }

/** Значение «печатать на системном принтере»: своего имени у него нет. */
private const val SYSTEM_PRINTER = "SYSTEM"
