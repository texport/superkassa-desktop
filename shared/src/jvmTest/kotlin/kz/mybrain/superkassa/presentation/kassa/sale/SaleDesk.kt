package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.kassa.CoreDesk
import kotlin.test.assertTrue

/** Продажа окна на кассе рабочего места [CoreDesk]: экран открыт, как у кассира. */
internal fun CoreDesk.sale(): SaleViewModel = saleModel(services, kassaPorts).also { it.visit() }

/**
 * Позиция руками, как её набирает кассир: наименование, цена, количество,
 * единица по ОКЕИ и скидка на позицию.
 */
internal fun SaleViewModel.add(
    name: String,
    price: String,
    quantity: String = "1",
    unit: String = OKEI_PIECE,
    discount: Adjustment = Adjustment()
) {
    val draft = state.value.draft.copy(
        name = name,
        price = price,
        quantity = quantity,
        measureUnitCode = unit,
        discount = discount
    )
    entry.editDraft(draft)
    assertTrue(entry.addDraft(), "позиция «$name» не встала в чек: ${state.value.draft.problems}")
}

/** Документы открытой смены кассы, как их видит ядро. */
internal fun ReadyKassa.shiftDocuments(): List<FiscalDocumentResponse> {
    val shift = checkNotNull(api.getLocalOpenShift(kkmId, adminPin)) { "смена закрыта" }
    return api.listShiftDocuments(kkmId, shift.id, DOCUMENTS, 0, adminPin)
}

/** Чеки продажи открытой смены в ядре. */
internal fun ReadyKassa.sales(): List<FiscalDocumentResponse> = shiftDocuments().filter { it.docType == "SALE" }

/** Штука по ОКЕИ. */
internal const val OKEI_PIECE = "796"

/** Килограмм по справочнику единиц кассы. */
internal val OKEI_KILOGRAM: String = UnitOfMeasurement.KILOGRAM.code

private const val DOCUMENTS = 500
