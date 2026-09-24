package kz.mybrain.superkassa.domain.kassa.model.entry

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureItemResponse
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.defaultVatOf
import kz.mybrain.superkassa.domain.kassa.model.sale.vatCodesOf

/**
 * Найденное в справочнике — сразу позиция: одна штука по цене справочника.
 *
 * Отсутствующая цена не делает находку ненайденной: цена остаётся нулевой,
 * и её спрашивают у кассира. Ставку и единицу справочник называет
 * не всегда; тогда берутся ставка кассы и штука, а не «Без НДС» и пустое
 * место: у плательщика НДС каждый отсканированный товар иначе уходил бы
 * в чек необлагаемым.
 *
 * @param kkm касса, за которой продают: её режим и ставка по умолчанию.
 * @param vatRates справочник ставок кассы.
 */
fun positionOf(item: NomenclatureItemResponse, kkm: KkmResponse?, vatRates: List<VatRateResponse>): Position =
    Position(
        name = item.name,
        price = Tenge.of(item.price).coerceAtLeast(0L),
        quantity = Decimal.ofScaled(1L, 0),
        vatGroup = item.vatGroup?.takeIf { it in vatCodesOf(kkm, vatRates) } ?: defaultVatOf(kkm, vatRates),
        measureUnitCode = item.measureUnitCode?.takeIf { it.isNotBlank() } ?: PIECE,
        nameKk = item.nameKk?.takeIf { it.isNotBlank() },
        ntin = item.ntin?.takeIf { it.isNotBlank() },
        barcode = item.barcode.takeIf { it.isNotBlank() }
    )

/**
 * Код товара, как его набрали или прислал сканер.
 *
 * Справочник ищет и по штрихкоду — цифрам, — и по маркировочному коду
 * товара, в котором бывают буквы: прежде поле пропускало только цифры,
 * и код с буквами до справочника не доходил. Отбрасываются только
 * пробелы и переводы строк: сканер дописывает их к коду сам.
 */
fun barcodeOf(typed: String): String = typed.filterNot(Char::isWhitespace)
