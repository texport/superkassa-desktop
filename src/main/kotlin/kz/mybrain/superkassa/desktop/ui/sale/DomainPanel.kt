package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Реквизиты отрасли, в которой работает эта касса.
 *
 * Отрасль выбрана в настройках кассы, и спрашиваются только её поля:
 * у такси — номер машины, тариф и признак заказа, у стоянки — время
 * въезда и выезда, у услуг — лицевой счёт, у нефтепродуктов — номер
 * карты. У кассы в торговле блока нет вовсе: заполнять ей нечего,
 * а прежде кассир магазина видел на экране выбор отрасли и поля,
 * которых не понимал.
 *
 * Карточка не сворачивается, в отличие от соседних: без этих полей
 * чек не пробить, и спрятать их значило бы погасить кнопку так,
 * что причина осталась бы за свёрнутым заголовком.
 *
 * Названием служит сама отрасль: кассир видит, чем эта касса торгует,
 * не заходя в настройки.
 */
@Composable
fun DomainCard(session: Session, form: SaleForm) {
    val kind = session.domain
    if (kind.fields.isEmpty()) return
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            SectionTitle(kind.title(texts.enums))
            DomainFields(kind, form.domain) { form.domain = it }
            // Упрёк — только о том, что кассир уже набрал: «Заполните:
            // Номер машины» над нетронутой карточкой ругает за работу,
            // которую он ещё не начинал. Незаполненное назовёт строка
            // под кнопкой, где причина стоит всегда.
            Hint(
                problem = form.domain.spoiled(kind)?.reason(extra),
                hint = extra.domainHint
            )
        }
    }
}

/** Поля выбранной отрасли — и ни одного чужого. */
@Composable
private fun DomainFields(kind: DomainKind, input: DomainInput, onChange: (DomainInput) -> Unit) {
    val texts = LocalSaleTexts.current
    val missing = input.missing(kind)
    kind.fields.forEach { field ->
        DomainTextField(
            value = input.value(field),
            label = field.label(texts),
            missing = missing == field
        ) { onChange(input.with(field, it)) }
    }
    if (kind == DomainKind.Taxi) OrderChip(input, onChange)
}

/**
 * Поездка по заказу или с улицы.
 *
 * Плашкой, а не выключателем: признак уходит в чек всегда, и оба его
 * значения — настоящий ответ, а не «включено и по умолчанию».
 */
@Composable
private fun OrderChip(input: DomainInput, onChange: (DomainInput) -> Unit) {
    val texts = LocalSaleTexts.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        FilterChip(
            selected = input.isOrder,
            onClick = { onChange(input.copy(isOrder = !input.isOrder)) },
            label = { Text(texts.byOrder) },
            leadingIcon = if (input.isOrder) {
                { Icon(AppIcons.chosen, contentDescription = null) }
            } else {
                null
            }
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
