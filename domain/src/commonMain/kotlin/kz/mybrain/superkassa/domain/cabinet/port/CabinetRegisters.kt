package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.domain.cabinet.model.RegisterEdit
import kz.mybrain.superkassa.domain.cabinet.model.RegisterName
import kz.mybrain.superkassa.domain.cabinet.model.TokenIssued
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState

/** Кассы компании в кабинете. */
interface CabinetRegisters {

    /** Все кассы; [onPart] — как у точек. */
    suspend fun all(onPart: (List<CabinetRegister>, Long) -> Unit = { _, _ -> }): List<CabinetRegister>

    /** Карточка кассы: в ней есть то, чего нет в строке списка, — признак карты и последнее действие. */
    suspend fun one(id: String): CabinetRegister

    suspend fun add(register: RegisterCreate): CabinetRegister

    suspend fun edit(id: String, edit: RegisterEdit): CabinetRegister

    /** Своё название владельца: обязательное, стереть его кабинет не даёт (см. [RegisterName]). */
    suspend fun rename(id: String, name: String): CabinetRegister

    suspend fun remove(id: String)

    /** Состояние кассы по учёту кабинета и по снимку БФД. */
    suspend fun state(id: String): RegisterState

    /**
     * Какие кассы кабинет считает заблокированными.
     *
     * В списке касс блокировки нет вовсе: кабинет отдаёт её сводкой по всем
     * кассам компании, одним обращением на весь список.
     */
    suspend fun blocked(): Set<String>

    /** Выпускает новый технический токен: прежний БФД отзывает. */
    suspend fun issueToken(id: String): TokenIssued

    /** Справочник моделей касс целиком. */
    suspend fun models(): List<KkmModel>
}
