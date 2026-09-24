package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.keyboard.onEnter
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.component.Hint
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.lookupProblemWords
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
fun BarcodeField(state: SaleUiState, actions: EntryActions) {
    Column(
        modifier = Modifier.fillMaxWidth().onEnter { actions.search() },
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        BarcodeInput(state, actions)
        BarcodeHint(state)
    }
    state.search.asking?.let { found ->
        PriceAskDialog(found, LocalUnits.current, onAdd = actions::addPriced, onDismiss = actions::dismissPriced)
    }
}

/** Поле кода и значок поиска в нём: сканер дописывает Enter сам. */
@Composable
private fun BarcodeInput(state: SaleUiState, actions: EntryActions) {
    val texts = LocalStrings.current
    OutlinedTextField(
        value = state.search.barcode,
        onValueChange = actions::typeBarcode,
        label = { Text(texts.sale.barcode) },
        singleLine = true,
        trailingIcon = {
            IconButton(enabled = state.search.ready && state.kkm != null, onClick = { actions.search() }) {
                Icon(AppIcons.find, contentDescription = texts.sale.barcodeFind)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
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
            problem != null -> lookupProblemWords(problem, state.kkm.blockReasonCode, LocalLanguage.current, texts.sale)
            else -> null
        },
        hint = if (state.search.searching) texts.sale.barcodeSearching else null
    )
}
