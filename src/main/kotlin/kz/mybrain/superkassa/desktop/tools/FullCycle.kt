package kz.mybrain.superkassa.desktop.tools

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.CashRequest
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.KkmUserRequest
import kz.mybrain.superkassa.desktop.server.addUser
import kz.mybrain.superkassa.desktop.server.cashIn
import kz.mybrain.superkassa.desktop.server.cashOut
import kz.mybrain.superkassa.desktop.server.changeUserPin
import kz.mybrain.superkassa.desktop.server.closeShift
import kz.mybrain.superkassa.desktop.server.initKkm
import kz.mybrain.superkassa.desktop.server.openShift
import kz.mybrain.superkassa.desktop.server.printImage
import kz.mybrain.superkassa.desktop.server.users
import kz.mybrain.superkassa.desktop.server.xReport
import java.math.BigDecimal

/**
 * Полный цикл кассы через тот же код, который вызывает интерфейс.
 *
 * Прогон нужен, чтобы проверять не сервер, а само приложение: он ходит
 * через его клиента, его модели и его сеанс. Если чек соберётся здесь,
 * он соберётся и по нажатию кнопки.
 *
 * Запуск: ./gradlew fullCycle -PsystemId=... -Ptoken=...
 */
object FullCycle {

    @JvmStatic
    fun main(args: Array<String>) {
        val systemId = args.getOrNull(0) ?: error("Не указан идентификатор кассы")
        val token = args.getOrNull(1) ?: error("Не указан токен")
        runBlocking { run(systemId, token) }
    }

    private suspend fun run(systemId: String, token: String) {
        val session = Session()
        val report = CycleReport()

        val kkm = session.guard("Заведение кассы") {
            session.client.initKkm(
                KkmInitRequest("BFD", "DEV", systemId, token, WORK_PIN),
                DEFAULT_PIN
            )
        }
        val registered = kkm ?: session.findExisting(systemId)
        if (registered == null) {
            report.fail("Заведение кассы", session.lastMessage.toString())
            report.print()
            return
        }
        report.done("Заведение кассы", "состояние ${registered.state}")
        session.select(registered)

        preparePin(session, registered.kkmId, report)
        session.adoptPin(WORK_PIN)

        report.step(session, "Открытие смены") { session.client.openShift(registered.kkmId, WORK_PIN) }
        ReceiptScenarios.all(session, registered.kkmId, report)
        moveCash(session, registered.kkmId, report)
        report.step(session, "X-отчёт") { session.client.xReport(registered.kkmId, WORK_PIN) }
        checkPrintForms(session, registered.kkmId, report)
        report.step(session, "Закрытие смены") { session.client.closeShift(registered.kkmId, WORK_PIN) }

        session.refreshSelected()
        report.summary(session.documents)
        report.print()
    }

    /**
     * Доводит пины кассы до рабочих.
     *
     * Заведённая этим прогоном касса уже родилась с рабочим пином; здесь
     * остаётся случай кассы, заведённой раньше — со стандартным.
     */
    private suspend fun preparePin(session: Session, kkmId: String, report: CycleReport) {
        val ready = session.guard("Кассиры") { session.client.users(kkmId, WORK_PIN) }
        if (ready != null) {
            report.done("Смена пина администратора", "не потребовалась")
            return
        }
        val existing = session.guard("Кассиры") { session.client.users(kkmId, DEFAULT_PIN) }
        val admin = existing?.firstOrNull { it.role == "ADMIN" }
        if (admin != null) {
            val changed = session.guard<Unit>("Смена пина") {
                session.client.changeUserPin(kkmId, admin.identifier, WORK_PIN, DEFAULT_PIN)
            }
            report.done("Смена пина администратора", if (changed != null) "выполнена" else "не потребовалась")
            return
        }
        session.guard("Заведение кассира") {
            session.client.addUser(kkmId, KkmUserRequest("Кассир", "CASHIER", WORK_PIN), DEFAULT_PIN)
        }
        report.done("Заведение кассира", "выполнено")
    }

    private suspend fun moveCash(session: Session, kkmId: String, report: CycleReport) {
        report.step(session, "Внесение наличных") {
            session.client.cashIn(kkmId, CashRequest(BigDecimal("5000.0"), key("in")), WORK_PIN)
        }
        report.step(session, "Изъятие наличных") {
            session.client.cashOut(kkmId, CashRequest(BigDecimal("1500.0"), key("out")), WORK_PIN)
        }
    }

    /** Печатная форма запрашивается по каждому документу смены. */
    private suspend fun checkPrintForms(session: Session, kkmId: String, report: CycleReport) {
        session.refreshSelected()
        delay(SETTLE_MILLIS)
        var printed = 0
        var missing = 0
        session.documents.forEach { document: Document ->
            val image = session.guard("Печать") {
                session.client.printImage(kkmId, document.id, WORK_PIN)
            }
            if (image != null && image.isNotEmpty()) printed++ else missing++
        }
        if (missing == 0) {
            report.done("Печатные формы", "получены по всем $printed документам")
        } else {
            report.fail("Печатные формы", "нет формы у $missing из ${printed + missing}")
        }
    }

    private fun key(tag: String) = "cycle-$tag-${System.currentTimeMillis()}"

    const val DEFAULT_PIN = "0000"
    const val WORK_PIN = "4821"
    private const val SETTLE_MILLIS = 500L
}
