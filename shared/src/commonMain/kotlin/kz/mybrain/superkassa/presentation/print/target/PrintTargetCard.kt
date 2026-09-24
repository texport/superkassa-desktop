package kz.mybrain.superkassa.presentation.print.target

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.LabelledPicker
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.common.section.PartTitle
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.print.printTexts

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
 */
@Composable
internal fun PrintTargetCard(target: PrintTargetUiState, actions: PrintTargetActions) {
    val texts = LocalStrings.current
    SectionCard(title = texts.settings.printer, info = texts.settings.printerHint) {
        if (target.systemDialog) {
            SystemDialogNote()
        } else {
            PrinterChoice(target, actions)
        }
        PartTitle(texts.settings.printKind)
        WideChoiceSegments(
            options = PrintKind.entries,
            selected = target.kind,
            label = { it.name.uppercase() },
            onSelect = actions::chooseKind
        )
    }
}

/**
 * Принтер и копии кассы — там, где машина видит принтеры по имени.
 *
 * Принтеров на машине может не быть вовсе — за прилавком это обычное дело
 * до подключения чекового. Молчание здесь кончалось отказом печати на первом
 * же чеке, при покупателе.
 */
@Composable
private fun PrinterChoice(target: PrintTargetUiState, actions: PrintTargetActions) {
    val texts = LocalStrings.current
    if (target.noPrinters) Warning(texts.settings.printerNone)
    if (target.printerGone) Warning(printTexts(LocalLanguage.current).printerGone)
    PrinterPicker(target, actions)
    PartTitle(texts.settings.printCopies)
    WideChoiceSegments(
        options = target.copyChoices,
        selected = target.copies,
        label = { it.toString() },
        onSelect = actions::chooseCopies
    )
}

/**
 * Печать системным диалогом — на Android.
 *
 * Принтера по имени здесь нет: печать открывает диалог системы, и принтер,
 * копии и «Сохранить как PDF» выбирают в нём. Выбор принтера и копий на
 * карточке ни на что бы не влиял — вместо них сказано, как печать идёт.
 */
@Composable
private fun SystemDialogNote() {
    Text(
        text = printTexts(LocalLanguage.current).systemDialog,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Выбор принтера кассы.
 *
 * Поле во всю ширину карточки, как и прочие выборы настроек: имя принтера
 * бывает длиннее заданной ширины, и выбор обрезался многоточием рядом
 * с пустой половиной карточки. Системный стоит первым: он же
 * и подставляется по умолчанию.
 */
@Composable
private fun PrinterPicker(target: PrintTargetUiState, actions: PrintTargetActions) {
    val texts = LocalStrings.current.settings
    LabelledPicker(
        label = texts.printer,
        options = listOf(Printer(null)) + target.printers.map(::Printer),
        selected = Printer(target.printer),
        title = { it?.name ?: texts.printerSystem },
        onSelect = { actions.choosePrinter(it.name) }
    )
}

/** Строка беды над выбором: цвет отказа, чтобы её не пропустили. */
@Composable
private fun Warning(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

/** Принтер в списке выбора; без имени — системный по умолчанию. */
private data class Printer(val name: String?)

/** Карточка принтера кассы со своей моделью — для настроек, которые о печати не знают. */
@Composable
fun PrintTargetSetting(app: AppContainer) {
    val model = printTargetViewModel(app)
    val state by model.state.collectAsScreenState()
    PrintTargetCard(state, model)
}
