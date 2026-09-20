package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Отраслевые реквизиты чека.
 *
 * Показываются только поля выбранного вида отрасли: ОФД принимает ровно
 * один подблок, и предлагать кассиру заполнить два — значит получить отказ.
 * Незаполненное обязательное поле названо поимённо: узел такой чек
 * пропускает, а ОФД отвергает, когда исправлять уже нечего.
 *
 * Поля идут столбцом во всю ширину кассовой колонки: строка из выбора
 * отрасли и двух реквизитов в неё не помещается.
 */
@Composable
fun DomainPanel(input: DomainInput, onChange: (DomainInput) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DomainKindPicker(input.kind) { onChange(input.copy(kind = it)) }
        DomainFields(input, onChange)
        Hint(
            problem = input.missing?.let { "${extra.fillIn}: ${it.label(texts.sale)}" },
            // Торговле заполнять нечего, и звать её к «реквизитам отрасли»
            // значит отправить кассира искать поле, которого нет.
            hint = when (input.kind) {
                DomainKind.Trading -> null
                DomainKind.Parking -> extra.parkingHint
                else -> texts.sale.domainHint
            }
        )
    }
}

@Composable
private fun DomainKindPicker(selected: DomainKind, onSelect: (DomainKind) -> Unit) {
    val texts = LocalStrings.current
    LabelledPicker(
        label = texts.sale.domainKind,
        options = DomainKind.entries,
        selected = selected,
        title = { kind -> kind?.title(texts.enums).orEmpty() },
        onSelect = onSelect
    )
}

/** Поля выбранной отрасли — и ничего сверх них. */
@Composable
private fun DomainFields(input: DomainInput, onChange: (DomainInput) -> Unit) {
    val texts = LocalStrings.current.sale
    when (input.kind) {
        DomainKind.Trading -> Unit
        DomainKind.Services, DomainKind.Hotels -> DomainTextField(
            value = input.accountNumber,
            label = texts.accountNumber,
            missing = input.missing == DomainField.AccountNumber
        ) { onChange(input.copy(accountNumber = it)) }
        DomainKind.GasOil -> DomainTextField(
            value = input.cardNumber,
            label = texts.cardNumber,
            missing = input.missing == DomainField.CardNumber
        ) { onChange(input.copy(cardNumber = it)) }
        DomainKind.Taxi -> TaxiFields(input, onChange)
        DomainKind.Parking -> DomainTextField(
            value = input.parkingHours,
            label = texts.parkingHours,
            missing = input.missing == DomainField.ParkingHours
        ) { onChange(input.copy(parkingHours = it.filter(Char::isDigit))) }
    }
}

@Composable
private fun TaxiFields(input: DomainInput, onChange: (DomainInput) -> Unit) {
    val texts = LocalStrings.current.sale
    DomainTextField(
        value = input.carNumber,
        label = texts.carNumber,
        missing = input.missing == DomainField.CarNumber
    ) { onChange(input.copy(carNumber = it)) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input.currentFee,
            onValueChange = { onChange(input.copy(currentFee = it)) },
            label = { Text(texts.fee) },
            singleLine = true,
            isError = input.missing == DomainField.Fee && input.currentFee.isNotBlank(),
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = input.isOrder,
            onClick = { onChange(input.copy(isOrder = !input.isOrder)) },
            label = { Text(texts.byOrder) },
            leadingIcon = if (input.isOrder) {
                { Icon(AppIcons.chosen, contentDescription = null) }
            } else {
                null
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/** Поле краснеет, только когда в нём что-то есть: пустое ещё не ошибка ввода. */
@Composable
private fun DomainTextField(
    value: String,
    label: String,
    missing: Boolean,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = missing && value.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    )
}
