package kz.mybrain.superkassa.desktop.ui.sale

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.components.onEnter
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ввод позиции чека руками.
 *
 * Поля стоят столбцом по ширине кассовой колонки, а не в одну длинную
 * строку: строка из шести полей на узкой колонке не помещается, и кассир
 * искал бы «Количество» за краем экрана.
 *
 * Enter добавляет позицию из любого поля формы: за кассой руки заняты
 * товаром, и тянуться к мыши ради каждой строки чека — потерянное время.
 * Кнопка недоступна ровно тогда, когда введённое ещё не образует позицию,
 * и строка над ней всегда называет, чего не хватает. Добавление —
 * второстепенное действие экрана, поэтому кнопка тональная: главное
 * действие здесь одно, и это «Пробить чек».
 */
@Composable
fun AddPositionForm(session: Session, onAdd: (Position) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val vat = defaultVatOf(session, LocalVatRates.current)
    val units = session.units
    // Штука — то, чем торгуют чаще всего, и она же подставляется узлом.
    // Ставим её явно: подставленное узлом кассир на экране не видит.
    val unit = remember(units) { units.firstOrNull { it.code == PIECE }?.code }
    var draft by remember(vat, unit) {
        mutableStateOf(PositionDraft(vatGroup = vat, measureUnitCode = unit))
    }
    val submit: () -> Boolean = {
        val ready = draft.position
        if (ready != null) {
            onAdd(ready)
            draft = draft.cleared()
        }
        ready != null
    }

    Column(
        modifier = Modifier.fillMaxWidth().onEnter { submit() },
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DraftFields(draft, units) { draft = it }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                enabled = draft.position != null,
                onClick = { submit() },
                modifier = Modifier.weight(1f)
            ) { Text(texts.sale.add, style = MaterialTheme.typography.titleSmall) }
        }
        Hint(draft.problems.firstOrNull()?.text(extra), extra.addByEnter)
    }
}

/** Поля позиции сверху вниз: что, почём, сколько, по какой ставке. */
@Composable
private fun DraftFields(
    draft: PositionDraft,
    units: List<UnitOfMeasurement>,
    onChange: (PositionDraft) -> Unit
) {
    val texts = LocalStrings.current
    OutlinedTextField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = { Text(texts.sale.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        DraftAmountField(draft, DraftField.Price, texts.sale.price) {
            onChange(draft.copy(price = it))
        }
        DraftAmountField(draft, DraftField.Quantity, texts.sale.quantity) {
            onChange(draft.copy(quantity = it))
        }
    }
    // Единица и скидка делят строку: с ценой единица в кассовую колонку
    // не влезает, а отдельной строкой она отодвигала «Добавить» под сгиб —
    // на окне ниже тысячи точек до кнопки приходилось прокручивать трижды,
    // и так на каждую позицию чека.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnitPicker(
            selected = draft.measureUnitCode,
            units = units,
            modifier = Modifier.weight(1f)
        ) { onChange(draft.copy(measureUnitCode = it)) }
        DraftAmountField(draft, DraftField.Discount, texts.sale.discount) {
            onChange(draft.copy(discount = it))
        }
    }
    // Ставка — своей строкой и только у плательщика НДС: у кассы без НДС
    // выбирать нечего, и строка не занимает высоту зря.
    VatPicker(draft.vatGroup, Modifier.fillMaxWidth()) { onChange(draft.copy(vatGroup = it)) }
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
    val value = draft.valueOf(field)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotBlank() && draft.problem(field) != null,
        modifier = Modifier.weight(1f)
    )
}
