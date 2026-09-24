package kz.mybrain.superkassa.integrations.bfdcabinet.register

import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.allPages
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query
import kotlin.uuid.Uuid

/**
 * Кассы компании: паспорт, состояние, технический токен и справочник моделей.
 *
 * Заявления по кассам — в `ApplicationsApi`.
 */
class RegistersApi internal constructor(private val link: CabinetLink, private val settings: CabinetSettings) {

    /** Одна страница касс. */
    suspend fun page(page: Int = 0): CabinetPage<CabinetRegister> =
        link.get(BASE + query("page" to page, "size" to CABINET_PAGE_SIZE))

    /**
     * Все кассы компании: кассу ищут по номеру КГД и заводскому среди всех.
     *
     * @param onPart прочитанное на сейчас и сколько касс всего.
     */
    suspend fun all(onPart: (List<CabinetRegister>, Long) -> Unit = { _, _ -> }): List<CabinetRegister> =
        allPages({ page(it) }, onPart)

    /** Карточка кассы: в ней есть признак карты и последнее действие. */
    suspend fun one(id: String): CabinetRegister = link.get(path(id))

    /** Заводит кассу черновиком. */
    suspend fun add(register: RegisterCreate): CabinetRegister = link.send(HttpMethod.Post, BASE, register)

    /** Правит заведённую кассу. */
    suspend fun edit(id: String, edit: RegisterEdit): CabinetRegister = link.send(HttpMethod.Patch, path(id), edit)

    /**
     * Своё название кассы.
     *
     * Стереть название кабинет не даёт: поле обязательно и не пустое
     * (1–120 знаков), и на пустое или отсутствующее он отвечает отказом
     * проверки полей. Поэтому название здесь всегда есть.
     */
    suspend fun rename(id: String, name: String): CabinetRegister =
        link.send(HttpMethod.Patch, "${path(id)}/internal-name", InternalNameRequest(name))

    /** Удаляет кассу. */
    suspend fun remove(id: String) = link.done(HttpMethod.Delete, path(id))

    /** Состояние кассы по учёту кабинета и по сервису приёма. */
    suspend fun state(id: String): RegisterState = link.get("${path(id)}/state")

    /**
     * Выпускает новый технический токен; прежний БФД отзывает.
     *
     * Значение кабинет показывает только после подтверждения сервиса приёма;
     * не успел — отвечает `PENDING`, и запрос повторяется с тем же ключом
     * идемпотентности: второго токена такой повтор не выпускает.
     *
     * @return токен; всё ещё [TokenIssued.pending] — подтверждения не дождались.
     */
    suspend fun issueToken(id: String): TokenIssued {
        val key = Uuid.random().toString()
        var issued = link.requestToken(path(id), key)
        var attempts = 1
        while (issued.pending && attempts < settings.tokenAttempts) {
            delay(settings.tokenPause)
            issued = link.requestToken(path(id), key)
            attempts++
        }
        return issued
    }

    /**
     * Справочник моделей касс целиком: модель ищут среди всех, а их сотни.
     */
    suspend fun models(): List<KkmModel> = allPages(
        { page -> link.get<CabinetPage<KkmModel>>(MODELS + query("page" to page, "size" to CABINET_PAGE_SIZE)) },
        { _, _ -> }
    )

    private companion object {
        const val MODELS = "/api/reference/kkm-models"
    }
}

/** Один вопрос о токене; ключ один на все повторы. */
private suspend fun CabinetLink.requestToken(register: String, key: String): TokenIssued = post("$register/token", key)

private fun path(id: String) = "$BASE/${id.inPath()}"

private const val BASE = "/api/cash-registers"
