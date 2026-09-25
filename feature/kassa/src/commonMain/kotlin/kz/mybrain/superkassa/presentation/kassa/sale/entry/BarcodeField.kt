package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kz.mybrain.superkassa.designsystem.keyboard.EnterSubmits
import kz.mybrain.superkassa.designsystem.keyboard.enterKeyboardActions
import kz.mybrain.superkassa.designsystem.keyboard.onEnter
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.component.Hint
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.words.kassa.lookupProblemWords

/**
 * Добавление позиции по штрихкоду.
 *
 * Справочник ведёт ОФД, поэтому цена, наименование и ставка НДС берутся
 * у него, а не вводятся кассиром: расхождение цены на кассе и в справочнике —
 * повод для претензии покупателя. Сканер сам дописывает Enter, поэтому
 * поиск начинается по Enter, а кнопка поиска живёт значком в самом поле:
 * отдельная кнопка рядом занимала бы место, которого в кассовой колонке нет.
 *
 * Цены в справочнике может не быть вовсе — национальный каталог её не
 * несёт. Такая позиция не встаёт в чек молча: цену и количество спрашивает
 * [PriceAskDialog]. Позиция с ценой добавляется сразу, как и прежде:
 * кассир сканирует и продолжает, не отвлекаясь.
 *
 * Ненайденный товар и неотвеченный справочник — разные беды, и говорят
 * о них по-разному: см. [LookupProblem]. Поиск делает модель продажи,
 * поле только показывает набранное и итог.
 */
@Composable
internal fun BarcodeField(state: SaleUiState, actions: EntryActions) {
    Column(
        modifier = Modifier.fillMaxWidth().onEnter { actions.search() },
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        EnterSubmits(actions::search) { BarcodeInput(state, actions) }
        BarcodeHint(state)
    }
    state.search.asking?.let { found ->
        PriceAskDialog(found, LocalUnits.current, onAdd = actions::addPriced, onDismiss = actions::dismissPriced)
    }
}

/**
 * Поле кода и значок поиска в нём: сканер дописывает Enter сам, а на
 * экранной клавиатуре поиск начинает её клавиша «Поиск».
 *
 * Штрихкод — цифры, и экранная клавиатура открывается цифровой: на
 * буквенной кассир искал цифры во втором ряду. Маркировочный код товара
 * бывает с буквами — их включает значок в поле. Значок стоит только при
 * вводе касанием: с клавиатурой и сканером выбирать нечего.
 */
@Composable
private fun BarcodeInput(state: SaleUiState, actions: EntryActions) {
    val texts = LocalStrings.current
    var letters by rememberSaveable { mutableStateOf(false) }
    val focus = barcodeFocus(state.barcodeTurn)
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    OutlinedTextField(
        value = state.search.barcode,
        onValueChange = actions::typeBarcode,
        label = { Text(texts.receipt.barcode) },
        singleLine = true,
        keyboardOptions = barcodeKeys(letters),
        keyboardActions = enterKeyboardActions(),
        trailingIcon = {
            Row {
                if (touch) LettersToggle(letters) { letters = !letters }
                IconButton(enabled = state.search.ready && state.kkm != null, onClick = { actions.search() }) {
                    Icon(AppIcons.find, contentDescription = texts.receipt.barcodeFind)
                }
            }
        },
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
    )
}

/**
 * Фокус поля штрихкода: позиция встала в чек или чек пробит — фокус здесь,
 * и следующий скан не уходит в «Принято». Первое появление поля фокуса
 * не берёт: экранная клавиатура не выскакивает сама.
 */
@Composable
private fun barcodeFocus(turn: Int): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(turn) { if (turn > 0) focus.requestFocus() }
    return focus
}

/** Цифровая клавиатура или буквенная; клавиша действия — «Поиск». */
private fun barcodeKeys(letters: Boolean): KeyboardOptions {
    val type = if (letters) KeyboardType.Ascii else KeyboardType.Number
    return KeyboardOptions(keyboardType = type, imeAction = ImeAction.Search)
}

/** Значок другой клавиатуры: на цифровой — буквы, на буквенной — цифры. */
@Composable
private fun LettersToggle(letters: Boolean, onToggle: () -> Unit) {
    val texts = LocalSaleTexts.current
    IconButton(onClick = onToggle) {
        if (letters) {
            Icon(AppIcons.keyboardDigits, contentDescription = texts.barcodeDigits)
        } else {
            Icon(AppIcons.keyboardLetters, contentDescription = texts.barcodeLetters)
        }
    }
}

/**
 * Строка под полем говорит только о деле: идёт поиск, товар не найден,
 * справочник молчит, касса заблокирована, кассы нет. Про то, что сканер
 * сам жмёт Enter, написано в подсказке заголовка — это правило, а не событие.
 */
@Composable
private fun BarcodeHint(state: SaleUiState) {
    val texts = LocalStrings.current
    val problem = state.search.problem
    Hint(
        problem = when {
            state.kkm == null -> LocalSaleTexts.current.blockNoKkm
            problem != null -> lookupProblemWords(
                problem,
                state.kkm.blockReasonCode,
                LocalLanguage.current,
                texts.receipt
            )
            else -> null
        },
        hint = if (state.search.searching) texts.receipt.barcodeSearching else null
    )
}
