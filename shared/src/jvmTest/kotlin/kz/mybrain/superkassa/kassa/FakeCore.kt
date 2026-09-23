package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.coroutines.Dispatchers
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.Kassa
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Фасад ядра для проверок: отвечает тем, что ему дали.
 *
 * Настоящее ядро кассу без ОФД не заводит, а проверкам нужны касса,
 * смена, документы и отказы по заказу. Фасад отвечает на названные
 * методы, отказывает названным кодом и падает сбоем на всё остальное:
 * молча отвечать пустотой за кассу он не должен.
 */
class FakeCore {
    private val answers = mutableMapOf<String, (List<Any?>) -> Any?>()

    /** Какие методы фасада звали, по порядку. */
    val calls: MutableList<String> = mutableListOf()

    /** На [method] фасад отвечает [answer]; доводы вызова — списком. */
    fun on(method: String, answer: (List<Any?>) -> Any?) {
        answers[method] = answer
    }

    /** На [method] фасад отказывает кодом [code] и словами на трёх языках. */
    fun refuse(method: String, code: String, ru: String = code, kk: String = code, en: String = code) =
        on(method) { throw ConflictException(TrilingualMessage(ru, kk, en), code) }

    val api: SuperkassaApi = Proxy.newProxyInstance(
        SuperkassaApi::class.java.classLoader,
        arrayOf(SuperkassaApi::class.java),
        InvocationHandler { _, method, args ->
            calls += method.name
            val answer = answers[method.name] ?: error("fake core has no answer for ${method.name}")
            answer(args?.toList().orEmpty())
        }
    ) as SuperkassaApi

    /** Касса поверх этого фасада; вызовы идут без смены потока. */
    fun kassa(): Kassa = EmbeddedKassa(api, Dispatchers.Unconfined)
}
