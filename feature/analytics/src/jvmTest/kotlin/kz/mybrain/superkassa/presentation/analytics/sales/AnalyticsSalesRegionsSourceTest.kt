package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.analytics.CabinetReplies
import kz.mybrain.superkassa.domain.analytics.model.regionsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Свод по регионам собирается и у владельца, открывшего аналитику первой.
 *
 * Регион стоит в адресе торговой точки, а в строках сводки адреса нет:
 * без справочника точек весь свод сходился в одну строку «Без адреса»
 * со ста процентами сети. Справочник читал только раздел торговых точек
 * кабинета, и сводка молча полагалась на то, что владелец туда заходил.
 */
class AnalyticsSalesRegionsSourceTest {

    @Test
    fun `сводка сама читает справочник точек`() {
        val cabinet = CabinetReplies.answering(::bodyFor)
        val asked = cabinet.asked
        val model = AnalyticsSalesViewModel(SalesCases(cabinet.analytics))
        val view = runBlocking {
            withContext(Dispatchers.Main) { model.follow("token") }
            model.state.first { it.reading.value != null }.reading.value
        }.let(::requireNonNull)

        assertTrue(
            asked.any { it.startsWith("/api/retail-places") },
            "справочник точек сводка не спросила: $asked"
        )
        assertEquals(listOf("Алматы"), view.retailPlaces.map { it.address?.substringBefore(",") })

        val regions = regionsOf(view.places, view.registers, view.retailPlaces, NO_ADDRESS)
        assertEquals(listOf("Алматы"), regions.map { it.title }, "свод сошёлся не в область, а в прочерк")
    }

    private fun <T : Any> requireNonNull(value: T?): T = requireNotNull(value) { "сводка не собралась" }

    private fun bodyFor(target: String): String = target.substringBefore('?').let { path ->
        when {
            path.startsWith("/api/retail-places") -> PLACES
            path.endsWith("/by-retail-place") -> SOLD
            path.endsWith("/by-cash-register") -> SOLD
            path.endsWith("/summary") -> SUMMARY
            path.endsWith("/documents") -> "{}"
            else -> "[]"
        }
    }

    private companion object {
        const val NO_ADDRESS = "Без адреса"

        const val PLACES = """{"totalElements":1,"items":[{"id":"p1","name":"Магазин на Достык",
            "address":"Алматы, Медеуский, Достык, 10"}]}"""

        const val SOLD = """{"retailPlaces":[{"retailPlaceId":"p1","name":"Магазин на Достык",
            "retailPlaceName":"Магазин на Достык","receiptCount":81,"revenue":61820.00}]}"""

        const val SUMMARY = """{"receiptCount":81,"revenue":61820.00,"cashRegisterCount":3294}"""
    }
}
