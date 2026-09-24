package kz.mybrain.superkassa.presentation.kassa.payment.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kz.mybrain.superkassa.presentation.common.picker.LabelledPicker
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.EnumStrings

/**
 * Выбор вида оплаты — выпадающим списком.
 *
 * Шесть плашек занимали две-три строки в самом низу кассовой колонки,
 * а не хватало этой высоты вводу товара: карточка позиции сжималась так,
 * что цену и ставку приходилось доставать прокруткой внутри карточки.
 * Список отдаёт высоту вводу, а выбранный вид виден в поле и без
 * открывания.
 *
 * Допустимость вида объявляет касса полем `supported`: непринимаемый вид
 * не прячется, а гаснет в списке. Почему он погас, говорит строка под
 * всеми оплатами — одна на все погасшие виды. Спрятать его значило бы
 * разойтись с кассой молча.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPicker(
    entries: List<PaymentTypeResponse>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    LabelledPicker(
        label = texts.sale.payment,
        options = entries,
        selected = entries.firstOrNull { it.code == selectedCode },
        title = { entry -> paymentTitle(entries, language, entry?.code.orEmpty(), texts.enums) },
        onSelect = { onSelect(it.code) },
        modifier = modifier,
        available = { it.supported }
    )
}

/** Название вида оплаты: сначала от кассы, потом своё. */
private fun paymentTitle(
    entries: List<PaymentTypeResponse>,
    language: Language,
    code: String,
    texts: EnumStrings
): String {
    val fromKassa = entries.firstOrNull { it.code == code }?.name?.of(language)
    return fromKassa?.takeIf { it != code } ?: texts.paymentFallback(code) ?: code
}
