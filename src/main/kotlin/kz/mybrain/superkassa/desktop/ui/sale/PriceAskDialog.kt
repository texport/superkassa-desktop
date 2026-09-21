package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.FormDialog
import kz.mybrain.superkassa.desktop.ui.components.onEnter
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Вопрос о цене позиции, найденной в каталоге без цены.
 *
 * Окно отвечает тот, кто стоит за кассой: цену в каталоге никто за него
 * не задаст, и отдельных прав приложение на это не спрашивает.
 *
 * Руки кассира остаются на клавиатуре — сканер её не отпускает: при
 * открытии набирается цена, Tab переводит к количеству, Enter добавляет,
 * Escape отказывает и не добавляет ничего. Количество подставлено единицей:
 * штучный товар — обычный случай.
 */
@Composable
fun PriceAskDialog(
    found: Position,
    units: List<UnitOfMeasurement>,
    onAdd: (Position) -> Unit,
    onDismiss: () -> Unit
) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val typed = remember(found) { AskedInput() }
    val ask = PriceAsk(found, typed.price.text, typed.counted.text)
    val add: () -> Boolean = {
        val ready = ask.position
        ready?.let(onAdd)
        ready != null
    }

    FormDialog(
        title = extra.priceAsk,
        icon = AppIcons.price,
        action = texts.sale.add,
        close = extra.priceAskCancel,
        busy = false,
        missing = ask.problems.map { it.text(extra) },
        onDismiss = onDismiss,
        onAction = { add() }
    ) {
        FoundItem(found, units)
        // Почему окно вообще открылось: кассир не ошибся, цены нет
        // в самом каталоге.
        Hint(null, extra.priceAskHint)
        AskedAmounts(ask, typed, add, onDismiss)
    }
}

/**
 * Набираемое в окне вместе с положением курсора.
 *
 * Курсор нужен самому окну: набранное в количестве обязано затирать
 * подставленную единицу, а не дописываться к ней.
 */
private class AskedInput {
    var price by mutableStateOf(TextFieldValue())
    var counted by mutableStateOf(TextFieldValue(DEFAULT_QUANTITY))
}

/** Что именно добавляют: чем каталог назвал товар, его код и единица. */
@Composable
private fun FoundItem(found: Position, units: List<UnitOfMeasurement>) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        DetailLine(texts.sale.name, found.name)
        DetailLine(extra.priceAskCode, found.ntin)
        DetailLine(texts.sale.measureUnit, unitTitle(units, found.measureUnitCode))
    }
}

/**
 * Цена за единицу и количество.
 *
 * Нажатия ловит строка над полями, а не каждое поле: Enter должен
 * добавлять позицию из любого из них, а Escape — отказывать, где бы
 * ни стоял курсор.
 */
@Composable
private fun AskedAmounts(
    ask: PriceAsk,
    typed: AskedInput,
    onAdd: () -> Boolean,
    onDismiss: () -> Unit
) {
    val texts = LocalStrings.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().onEnter(onAdd).onEscape {
            onDismiss()
            true
        },
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        AskedField(ask, DraftField.Price, typed.price, texts.sale.price, Modifier.focusRequester(focus)) {
            typed.price = it
        }
        AskedField(ask, DraftField.Quantity, typed.counted, texts.sale.quantity, Modifier) {
            typed.counted = it
        }
    }
}

/**
 * Поле числа с причиной отказа под ним.
 *
 * Причина стоит у своего поля, а не общей строкой окна: «дробного
 * количества у штуки не бывает» под ценой кассир прочитал бы про цену.
 * Красным поле становится, только когда в нём что-то набрано: пустое
 * окно при открытии не должно выглядеть набором ошибок.
 *
 * Получив ввод, поле выделяет набранное целиком: «1,45» поверх
 * подставленной единицы давало «1,451» — тысячу четыреста пятьдесят
 * одну штуку вместо полутора килограммов.
 */
@Composable
private fun RowScope.AskedField(
    ask: PriceAsk,
    field: DraftField,
    value: TextFieldValue,
    label: String,
    modifier: Modifier,
    onChange: (TextFieldValue) -> Unit
) {
    val extra = LocalSaleTexts.current
    val problem = ask.problem(field)?.takeIf { value.text.isNotBlank() }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = if (problem == null) null else ({ Text(problem.text(extra)) }),
        isError = problem != null,
        singleLine = true,
        modifier = modifier.weight(1f).onFocusChanged { state ->
            if (state.isFocused) onChange(value.copy(selection = TextRange(0, value.text.length)))
        }
    )
}
