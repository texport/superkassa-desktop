package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetLogin
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetMe
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser

/**
 * Выданный кабинетом доступ и тот, кому он выдан.
 *
 * Отдельный предмет от сеанса: доступ, владелец и компания появляются
 * и исчезают всегда втроём — входом, выходом и истёкшим сроком. Пока они
 * лежали тремя полями сеанса, каждое такое место гасило их по отдельности,
 * и забытое поле оставляло бы имя вошедшего на экране после выхода.
 *
 * Доступ живёт только в памяти приложения: на диск он не пишется.
 */
class CabinetAccess {

    /** Выданный кабинетом доступ; `null` — владелец не входил. */
    var token: String? by mutableStateOf(null)
        private set

    var user: CabinetUser? by mutableStateOf(null)
        private set

    var company: CabinetCompany? by mutableStateOf(null)
        private set

    /** Вход по ЭЦП: кабинет выдал доступ и назвал вошедшего. */
    fun enter(login: CabinetLogin) = keep(login.accessToken, login.user, login.company)

    /** Вход без ЭЦП: доступ заменён отметкой, а вошедшего кабинет назвал по личности. */
    fun enter(issued: String, entered: CabinetMe) = keep(issued, entered.user, entered.company)

    /** Доступа больше нет: отозван выходом или истёк. */
    fun forget() {
        token = null
        user = null
        company = null
    }

    private fun keep(issued: String, who: CabinetUser, whose: CabinetCompany) {
        token = issued
        user = who
        company = whose
    }
}
