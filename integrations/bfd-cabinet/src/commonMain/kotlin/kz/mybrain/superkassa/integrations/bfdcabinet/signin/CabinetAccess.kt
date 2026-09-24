package kz.mybrain.superkassa.integrations.bfdcabinet.signin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetExpired
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetFailure
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetRefusal

/**
 * Выданный кабинетом доступ и тот, кому он выдан.
 *
 * Доступ живёт только в памяти и наружу модуля не выходит: приложение знает,
 * кто вошёл, но ключа не видит и в журнал отдать его не может. Доступ
 * и вошедший появляются и исчезают вместе — входом, выходом и истёкшим
 * сроком, — иначе имя вошедшего осталось бы на экране после выхода.
 */
internal class CabinetAccess {
    private val token = MutableStateFlow<String?>(null)
    private val entered = MutableStateFlow<CabinetOwner?>(null)

    val owner: StateFlow<CabinetOwner?> = entered.asStateFlow()

    /** Доступ сейчас; `null` — никто не входил. */
    val current: String? get() = token.value

    /**
     * Обращение от имени вошедшего.
     *
     * 401 при выданном доступе — конец сеанса, прочее — отказ по существу.
     *
     * @throws CabinetExpired доступа нет или кабинет его больше не принимает.
     */
    suspend fun <T> call(request: suspend (String) -> T): T {
        val access = token.value ?: throw CabinetExpired()
        return try {
            request(access)
        } catch (refusal: CabinetRefusal) {
            throw expiredOr(refusal)
        }
    }

    /** 401 при выданном доступе — конец сеанса: доступ забывается. */
    private fun expiredOr(refusal: CabinetRefusal): CabinetFailure {
        if (refusal.httpStatus != UNAUTHORIZED) return refusal
        forget()
        return CabinetExpired(refusal)
    }

    fun keep(issued: String, who: CabinetOwner) {
        token.value = issued
        entered.value = who
    }

    fun forget() {
        token.value = null
        entered.value = null
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
