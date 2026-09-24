package kz.mybrain.superkassa.domain.workplace.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Негодный адрес службы не уходит в настройки.
 *
 * Поле принимало любую непустую строку. Набранный без схемы
 * «192.168.50.35:17700» приложение не разбирает вовсе, а владельцу это
 * выходило как «служба недоступна»: он шёл искать сеть и сервер, тогда
 * как искать нужно было опечатку в поле рядом.
 */
class ServiceAddressTest {

    @Test
    fun `адрес без схемы и с пробелами внутри не годится`() {
        listOf(
            "http://127.0.0.1:8080",
            "https://bfd-cabinet.ecc.kz",
            "http://192.168.50.35:17700/"
        ).forEach { assertTrue(ServiceAddress.valid(it), "годный адрес отвергнут: $it") }

        listOf("", "   ", "192.168.50.35:17700", "localhost:8080", "не адрес", "http://", "http:// 1.2.3.4")
            .forEach { assertFalse(ServiceAddress.valid(it), "негодный адрес принят: $it") }
    }

    /** Косая черта на конце и пробелы по краям снимаются, остальное — как набрано. */
    @Test
    fun `адрес сохраняется без краёв и без черты на конце`() {
        assertEquals("http://192.168.50.35:17700", ServiceAddress.tidy("  http://192.168.50.35:17700/  "))
    }
}
