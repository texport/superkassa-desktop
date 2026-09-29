package kz.mybrain.superkassa.domain.cabinet.model

/**
 * Точки после того, как касса встала в список окна: заведённая или
 * перенесённая в другую точку.
 *
 * Число касс точки кабинет присылает вместе со списком точек, и оно
 * замирало на прочитанном: касса, заведённая здесь же, стояла под точкой,
 * а точка писала «Касс 0». Число сдвигается на кассу — прежней точке меньше,
 * новой больше, — а не пересчитывается по списку касс: считает его кабинет,
 * и своё правило подсчёта у него остаётся.
 *
 * @param before касса до изменения; `null` — её в списке не было.
 * @param after касса после изменения.
 */
fun List<RetailPlace>.withRegister(before: CabinetRegister?, after: CabinetRegister): List<RetailPlace> {
    val from = before?.retailPlace?.id
    val to = after.retailPlace?.id
    if (from == to) return this
    return map { place ->
        when (place.id) {
            from -> place.copy(cashRegisterCount = (place.cashRegisterCount - 1).coerceAtLeast(0))
            to -> place.copy(cashRegisterCount = place.cashRegisterCount + 1)
            else -> place
        }
    }
}
