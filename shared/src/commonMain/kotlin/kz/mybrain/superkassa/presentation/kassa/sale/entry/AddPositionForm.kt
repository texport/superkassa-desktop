package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.entry.DraftField
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.presentation.common.field.MoneyField
import kz.mybrain.superkassa.presentation.common.keyboard.EnterSubmits
import kz.mybrain.superkassa.presentation.common.keyboard.enterKeyboardActions
import kz.mybrain.superkassa.presentation.common.keyboard.onEnter
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.component.AdjustmentField
import kz.mybrain.superkassa.presentation.kassa.sale.component.Hint
import kz.mybrain.superkassa.presentation.kassa.sale.component.sameOtherwise
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.MeasureUnit
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.words.kassa.text

/**
 * Ввод позиции чека руками.
 *
 * Поля стоят столбцом по ширине кассовой колонки: строка из шести полей
 * на узкой колонке не помещается, и кассир искал бы «Количество» за краем.
 *
 * Enter (и «Готово» экранной клавиатуры) добавляет позицию из любого поля формы:
 * за кассой руки заняты товаром, и тянуться к мыши за каждой строкой некогда.
 * Кнопка недоступна ровно тогда, когда введённое ещё не образует позицию,
 * и строка над ней всегда называет, чего не хватает. Добавление —
 * второстепенное действие экрана, поэтому кнопка тональная: главное
 * действие здесь одно, и это «Пробить чек».
 */
@Composable
fun AddPositionForm(draft: PositionDraft, actions: EntryActions) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    Column(
        modifier = Modifier.fillMaxWidth().onEnter { actions.addDraft() },
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        EnterSubmits({ actions.addDraft() }) { DraftFields(draft, LocalUnits.current, actions::editDraft) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                enabled = draft.position != null,
                onClick = { actions.addDraft() },
                modifier = Modifier.weight(1f)
            ) { Text(texts.sale.add, style = MaterialTheme.typography.titleSmall) }
        }
        Hint(draft.hint?.text(extra), extra.addByEnter)
    }
}

/**
 * Поля позиции сверху вниз: что, почём, сколько, по какой ставке.
 *
 * Открыто пакету ради окна цены: оно спрашивает те же цену и количество.
 */
@Composable
internal fun DraftFields(
    draft: PositionDraft,
    units: List<MeasureUnit>,
    onChange: (PositionDraft) -> Unit
) {
    val texts = LocalStrings.current
    DraftName(draft, onChange)
    // Цена и количество делят строку поровну: края полей совпадают с краями
    // наименования и скидки над и под ними — одна сетка на всю форму.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        DraftAmountField(draft, DraftField.Price, texts.sale.price) {
            onChange(draft.copy(price = it))
        }
        DraftAmountField(draft, DraftField.Quantity, texts.sale.quantity) {
            onChange(draft.copy(quantity = it))
        }
    }
    // Скидка занимает строку целиком: внутри поля стоит выбор тенге или
    // доли, и на половине строки число прижималось к переключателю, а
    // подпись под полем переносилась на вторую строку.
    DraftDiscountField(draft, onChange)
    DraftChoices(draft, units, onChange)
}

/** Наименование: про нехватку сказано у самого поля. */
@Composable
private fun DraftName(draft: PositionDraft, onChange: (PositionDraft) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    // Начатая форма: кассир уже что-то набрал. Пустая форма молчит —
    // при открытии смены она не должна выглядеть списком недоделок.
    val nameProblem = draft.problem(DraftField.Name)?.takeIf { draft.started }
    OutlinedTextField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = { Text(texts.sale.name) },
        singleLine = true,
        // Про нехватку наименования сказано у самого поля. Строка под
        // кнопкой на кассовой колонке уезжает за сгиб, и кассир, набравший
        // цену без названия, видел лишь серую кнопку «Добавить».
        supportingText = nameProblem?.let { { Text(it.text(extra)) } },
        keyboardActions = enterKeyboardActions(),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Единица и ставка — выбором из готового. */
@Composable
private fun DraftChoices(draft: PositionDraft, units: List<MeasureUnit>, onChange: (PositionDraft) -> Unit) {
    // Два списка делят строку: выбирают они из готового, а не набирают,
    // и каждому хватает половины кассовой колонки. Порознь они отодвигали
    // «Добавить» под сгиб — на окне ниже тысячи точек до кнопки
    // приходилось прокручивать, и так на каждую позицию чека.
    //
    // Ставка есть только у плательщика НДС: у кассы без НДС выбирать
    // нечего, и тогда единица занимает строку целиком сама.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        UnitPicker(
            selected = draft.measureUnitCode,
            units = units,
            modifier = Modifier.weight(1f)
        ) { onChange(draft.copy(measureUnitCode = it)) }
        VatPicker(draft.vatGroup, Modifier.weight(1f)) {
            onChange(draft.copy(vatGroup = it))
        }
    }
}

/**
 * Скидка на позицию: тенге или доля — тем же полем, что и скидка на чек.
 *
 * Под полем стоит либо помеха, либо то же число другим способом: набравший
 * долю видит тенге, которые уйдут в кассу, и не пересчитывает их в уме
 * перед покупателем.
 */
@Composable
private fun DraftDiscountField(draft: PositionDraft, onChange: (PositionDraft) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val problem = draft.problem(DraftField.Discount)?.takeIf { draft.discount.text.isNotBlank() }
    AdjustmentField(
        label = texts.sale.discount,
        change = draft.discount,
        modifier = Modifier.fillMaxWidth(),
        isError = problem != null,
        supportingText = problem?.text(extra)
            ?: sameOtherwise(draft.discount, draft.lineCost, extra.lineChangeAsPercent),
        onEnter = { onChange(draft.copy(discount = draft.discount.copy(text = it))) },
        onSwitch = { onChange(draft.copy(discount = draft.discount.copy(unit = it))) }
    )
}

/**
 * Поле числа с подсветкой ошибки.
 *
 * Красным поле становится, только когда в нём что-то есть: пустая форма
 * при открытии смены не должна выглядеть набором ошибок.
 */
@Composable
private fun RowScope.DraftAmountField(
    draft: PositionDraft,
    field: DraftField,
    label: String,
    onChange: (String) -> Unit
) {
    val extra = LocalSaleTexts.current
    val value = draft.valueOf(field)
    val problem = draft.problem(field)?.takeIf { value.isNotBlank() }
    MoneyField(
        value = value,
        label = label,
        modifier = Modifier.weight(1f),
        isError = problem != null,
        // Помеха стоит под своим полем, а не только строкой под кнопкой:
        // на окне кассира форма позиции не влезает целиком, и строка под
        // кнопкой оказывалась за сгибом — красное поле кассир видел,
        // а причину нет и прокручивать её не догадывался.
        supportingText = problem?.text(extra),
        onValueChange = onChange
    )
}
