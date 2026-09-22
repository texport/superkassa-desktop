package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.NomenclatureItem
import kz.mybrain.superkassa.desktop.server.lookupBarcode
import kz.mybrain.superkassa.desktop.ui.components.onEnter
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

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
 * о них по-разному: см. [LookupProblem].
 *
 * @param added сколько позиций уже встало в чек: любая добавленная
 *              позиция очищает поле и снимает надпись о беде — товар
 *              заведён, и говорить о нём нечего.
 */
@Composable
fun BarcodeField(session: Session, added: Int, onFound: (Position) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val scope = rememberCoroutineScope()
    var barcode by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<LookupProblem?>(null) }
    var asking by remember { mutableStateOf<Position?>(null) }

    LaunchedEffect(added) {
        if (added > 0) {
            barcode = ""
            problem = null
        }
    }

    val kkm = session.selected
    val rates = LocalVatRates.current
    val ready = !searching && barcode.isNotBlank() && kkm != null
    val search: () -> Boolean = {
        if (ready) {
            searching = true
            scope.launch {
                val found = session.guard(texts.sale.barcodeSearch) {
                    session.client.lookupBarcode(kkm.kkmId, barcode, session.pin)
                }
                val position = found?.let { positionOf(it, rates, defaultVatOf(session, rates)) }
                // Позиция без цены уходит в окно, а не в чек: нулевую
                // строку кассир обязан заполнить прежде, чем она станет
                // частью фискального документа.
                when {
                    position == null -> Unit
                    priceMissing(position) -> asking = position
                    else -> onFound(position)
                }
                if (position != null) barcode = ""
                problem = if (position == null) lookupProblemOf(session) else null
                searching = false
            }
        }
        ready
    }

    Column(
        modifier = Modifier.fillMaxWidth().onEnter { search() },
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        OutlinedTextField(
            value = barcode,
            onValueChange = {
                barcode = it.filter(Char::isDigit)
                problem = null
            },
            label = { Text(texts.sale.barcode) },
            singleLine = true,
            trailingIcon = {
                IconButton(enabled = ready, onClick = { search() }) {
                    Icon(AppIcons.find, contentDescription = texts.sale.barcodeFind)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        val failed = problem
        Hint(
            problem = when {
                kkm == null -> extra.blockNoKkm
                failed != null -> lookupProblemWords(failed, session, texts.sale)
                else -> null
            },
            // Строка под полем говорит только о деле: идёт поиск, товар
            // не найден, справочник молчит, касса заблокирована, кассы нет.
            // Про то, что сканер сам жмёт Enter, написано в подсказке
            // заголовка — это правило, а не событие.
            hint = if (searching) texts.sale.barcodeSearching else null
        )
    }
    asking?.let { found ->
        PriceAskDialog(
            found = found,
            units = session.units,
            onAdd = {
                onFound(it)
                asking = null
            },
            onDismiss = { asking = null }
        )
    }
}

/**
 * Найденное в справочнике — сразу позиция: одна штука по цене справочника.
 *
 * Отсутствующая цена не делает находку ненайденной: каталог НКТ цен
 * не несёт вовсе, и «нет такого штрихкода» на такой товар было бы
 * неправдой. Цена остаётся нулевой, и её спрашивают у кассира.
 */
private fun positionOf(item: NomenclatureItem, rates: List<VatRate>, fallbackVat: String): Position {
    val price = item.sellPrice ?: BigDecimal.ZERO
    return Position(
        name = item.title,
        price = price,
        quantity = BigDecimal.ONE,
        // Ставку называет сам справочник; своей догадки по проценту здесь
        // больше нет — узел отдаёт код группы, а не число.
        // Справочник ставку называет не всегда; тогда берётся ставка самой
        // кассы, а не «Без НДС»: иначе у плательщика НДС каждый
        // отсканированный товар уходил бы в чек необлагаемым.
        vatGroup = item.vatGroup?.takeIf { code -> rates.any { it.code == code } } ?: fallbackVat,
        // Справочник единицу называет не всегда; тогда берётся штука,
        // а не пустое место: без единицы узел ставит её сам, и на экране
        // строка чека выглядела бы иначе, чем набранная руками.
        measureUnitCode = item.measureUnitCode?.takeIf { it.isNotBlank() } ?: PIECE,
        nameKk = item.nameKk?.takeIf { it.isNotBlank() },
        // НТИН приходит из справочника и уходит в ОФД: без него позиция
        // прослеживается только наименованием, набранным кассиром.
        ntin = item.ntin?.takeIf { it.isNotBlank() },
        barcode = item.barcode?.takeIf { it.isNotBlank() }
    )
}
