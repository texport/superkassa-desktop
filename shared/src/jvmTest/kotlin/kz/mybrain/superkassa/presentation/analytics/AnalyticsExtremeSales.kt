package kz.mybrain.superkassa.presentation.analytics

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Предельная торговая сводка: миллиарды тенге с тиынами, сто тысяч чеков,
 * девяносто дней на графике, пять тысяч касс и две тысячи точек в таблицах.
 */
internal object AnalyticsExtremeSales {

    private const val DAYS = 90L
    private val TODAY: LocalDate = LocalDate.of(2026, 9, 23)

    /** Сумма в миллиарды тенге с тиынами у первых строк, в миллионы — у остальных. */
    private fun billions(seed: Int): BigDecimal = if (seed < TOP) {
        BigDecimal("98797031109.99").subtract(BigDecimal(seed * 7_919L))
    } else {
        BigDecimal("1234567.89").add(BigDecimal(seed))
    }

    /** Сколько первых касс и точек торгуют на миллиарды; остальные — на миллионы. */
    private const val TOP = 20

    private fun unit(id: String, name: String, seed: Int, registration: String?, place: String?) = buildJsonObject {
        put(if (registration != null) "cashRegisterId" else "retailPlaceId", id)
        put(if (registration != null) "internalName" else "name", name)
        registration?.let { put("registrationNumber", it) }
        place?.let { put("retailPlaceName", it) }
        put("receiptCount", 100_000 - seed)
        put("revenue", JsonPrimitive(billions(seed)))
        put("net", JsonPrimitive(billions(seed).subtract(BigDecimal("1234567.89"))))
        put("lastContactAt", "2026-09-22T20:29:40Z")
    }

    fun registers(): JsonObject = buildJsonObject {
        put(
            "cashRegisters",
            JsonArray(
                (0 until AnalyticsFixtures.KKMS).map { index ->
                    unit(
                        "kkm-$index",
                        "Касса № ${index + 1} — кіреберістегі үлкен",
                        index,
                        "2609400${"%05d".format(index)}",
                        AnalyticsFixtures.placeName(index % AnalyticsFixtures.PLACES)
                    )
                }
            )
        )
    }

    fun places(): JsonObject = buildJsonObject {
        put(
            "retailPlaces",
            JsonArray(
                (0 until AnalyticsFixtures.PLACES).map { place ->
                    unit("place-$place", AnalyticsFixtures.placeName(place), place, null, null)
                }
            )
        )
    }

    fun summary(): String = buildJsonObject {
        put("receiptCount", 100_000)
        put("returnCount", 12_345)
        put("revenue", JsonPrimitive(BigDecimal("98797031109.99")))
        put("refunds", JsonPrimitive(BigDecimal("1234567890.12")))
        put("net", JsonPrimitive(BigDecimal("97562463219.87")))
        put("averageReceipt", JsonPrimitive(BigDecimal("987970.31")))
        put("taxTotal", JsonPrimitive(BigDecimal("10585396190.36")))
        put("purchaseCount", 100_000)
        put("purchases", JsonPrimitive(BigDecimal("9879703110.99")))
        put("purchaseRefunds", JsonPrimitive(BigDecimal("987970311.09")))
        put("payments", payments())
        put("offlineCount", 1234)
        put("queuedCount", 56_789)
        put("unknownCount", 100_000)
        put("cashRegisterCount", AnalyticsFixtures.KKMS)
        put("openShiftCount", 4321)
    }.toString()

    private fun payments() = buildJsonObject {
        put("cash", JsonPrimitive(BigDecimal("48797031109.99")))
        put("card", JsonPrimitive(BigDecimal("30000000000.00")))
        put("electronic", JsonPrimitive(BigDecimal("10000000000.00")))
        put("mobile", JsonPrimitive(BigDecimal("9000000000.00")))
        put("credit", JsonPrimitive(BigDecimal("1000000000.00")))
    }

    fun days(): String = buildJsonObject {
        put(
            "days",
            buildJsonArray {
                (0 until DAYS).forEach { back ->
                    add(
                        buildJsonObject {
                            put("date", TODAY.minusDays(DAYS - 1 - back).toString())
                            put("receiptCount", 1000 + back.toInt() * 10)
                            put("revenue", JsonPrimitive(BigDecimal("1097744790.11").add(BigDecimal(back * 1_000_000))))
                            put("refunds", JsonPrimitive(BigDecimal("13717421.00")))
                            put("net", JsonPrimitive(BigDecimal("1084027369.11")))
                        }
                    )
                }
            }
        )
    }.toString()

    fun hours(): String = buildJsonObject {
        put(
            "hours",
            JsonArray(
                (0 until 24).map { hour ->
                    buildJsonObject {
                        put("hour", hour)
                        put("receiptCount", 4000 + hour * 100)
                        put("revenue", JsonPrimitive(BigDecimal("4116542962.91")))
                    }
                }
            )
        )
    }.toString()

    fun documents(): String = buildJsonObject {
        put(
            "receipts",
            buildJsonObject {
                put("total", 100_000)
                put("delivered", 43_210)
                put("queued", 56_789)
                put("unknown", 1)
                put("rejected", 1234)
            }
        )
        put(
            "reports",
            buildJsonObject {
                put("total", 9_000)
                put("delivered", 8_000)
                put("queued", 900)
                put("unknown", 50)
                put("rejected", 50)
            }
        )
        put("offlineCount", 1234)
    }.toString()
}
