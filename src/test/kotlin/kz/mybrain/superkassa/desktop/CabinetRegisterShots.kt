package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.CashRegisterModel
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlaceRef
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.cabinet.RegisterPassport
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistersPage
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки раздела «Кассы»: список слева и паспорт выбранной кассы справа.
 *
 * Техническое состояние проверено отдельно (`TechnicalStateRenderTest`),
 * здесь смотрят на другое: объясняет ли себя пустой список, видно ли
 * по паспорту, что касса не на учёте или снята с него, и гаснет ли
 * выдача токена там, где кабинет её всё равно не даст.
 */
class CabinetRegisterShots {

    private fun stage(registers: String) = CabinetStage { path ->
        when {
            path == "/api/cash-registers" ->
                CabinetReply("""{"page":0,"size":50,"totalElements":1,"items":[$registers]}""")
            path == "/api/retail-places" -> CabinetReply("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
            else -> CabinetReply("{}")
        }
    }

    private fun register(status: String, number: String? = "000000010001") = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа",
        status = status,
        registrationNumber = number,
        factoryNumber = "SN-ECC-172758",
        manufactureYear = 2026,
        modelView = CashRegisterModel("0x0065000086cb", "«ПОРТ FPG-350 ФKZ»"),
        retailPlaceView = RetailPlaceRef("p-1", "Магазин на Абая"),
        registrationCardAvailable = true
    )

    private fun snapshot() = RegisterState(
        cashRegisterId = "r-1",
        businessStatus = "REGISTERED",
        technicalState = TechnicalState(
            found = true,
            active = true,
            shiftStatus = "CLOSED",
            shiftNumber = 42,
            lastContactAt = "2026-09-20T13:47:00Z"
        )
    )

    /** Паспорт кассы в той колонке, какая отведена ему в рабочем месте. */
    private fun passport(
        name: String,
        status: String,
        number: String? = "000000010001",
        here: Kkm? = null,
        problem: CabinetProblem? = null
    ): ByteArray {
        val stage = stage("")
        here?.let { stage.session.kkms.add(it) }
        return shot(name, width = CARD_WIDTH, height = CARD_HEIGHT) {
            val body = @Composable {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                    RegisterPassport(
                        stage.session,
                        stage.cabinet,
                        stage.texts,
                        register(status, number),
                        snapshot()
                    ) {}
                }
            }
            if (problem == null) body() else WithCabinetMessage(problem) { body() }
        }
    }

    @Test
    fun `пустой список касс объясняет, что делать, а список с кассой — нет`() {
        val stage = stage("")
        val empty = shot("registers-empty") { RegistersPage(stage.session, stage.cabinet, stage.texts) }

        val one = stage(
            """{"id":"r-1","kkmId":5000021,"internalName":"Касса у входа","status":"REGISTERED",
               "registrationNumber":"000000010001","factoryNumber":"SN-ECC-172758",
               "modelName":"«ПОРТ FPG-350 ФKZ»","retailPlaceId":"p-1","retailPlaceName":"Магазин на Абая"}"""
        )
        val listed = shot("registers-list") { RegistersPage(one.session, one.cabinet, one.texts) }

        assertTrue(empty.isNotEmpty() && listed.isNotEmpty())
        assertTrue(!empty.contentEquals(listed), "пустой список и список с кассой выглядят одинаково")
    }

    @Test
    fun `паспорт различает черновик, учёт, снятие и ожидание ответа КГД`() {
        val frames = mapOf(
            "register-draft" to passport("register-draft", "DRAFT", number = null),
            "register-registered" to passport("register-registered", "REGISTERED"),
            "register-deregistered" to passport("register-deregistered", "DEREGISTERED"),
            "register-in-isna" to passport("register-in-isna", "REGISTRATION_IN_ISNA_PROCESS")
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(frames.values.map { it.toList() }.distinct().size == frames.size, "состояния неотличимы")
    }

    /**
     * Касса этой машины, касса чужой машины и заблокированная.
     *
     * Узел знает только свои кассы, и сверка идёт по идентификатору
     * у БФД: сходится — касса здешняя, не сходится — работать с неё
     * отсюда нельзя, и предлагать переход было бы обманом.
     */
    @Test
    fun `работа на этой машине различает свою кассу, чужую и заблокированную`() {
        val mine = passport("register-here", "REGISTERED", here = node("ACTIVE"))
        val blocked = passport("register-here-blocked", "REGISTERED", here = node("BLOCKED"))
        val foreign = passport("register-elsewhere", "REGISTERED")

        assertTrue(!mine.contentEquals(foreign), "своя касса и чужая выглядят одинаково")
        assertTrue(!mine.contentEquals(blocked), "заблокированная касса не отличается от работающей")
    }

    private fun node(state: String) =
        Kkm(kkmId = "k-1", name = "Касса у входа", ofdSystemId = "5000021", state = state)

    /**
     * Токен, которого не выдадут, и отказ, если кнопку всё же нажали.
     *
     * Кабинет выдаёт ключ только кассе на учёте; по черновику кнопка
     * гаснет, а причина стоит строкой — иначе владелец жмёт и получает
     * отказ кодом по-английски.
     */
    @Test
    fun `выдача токена гаснет у кассы не на учёте`() {
        val locked = passport("register-token-locked", "DRAFT", number = null)
        val refused = passport(
            "register-token-refused",
            "DRAFT",
            number = null,
            problem = CabinetProblem.Refused(
                "CASH_REGISTER_STATUS",
                "Token is issued only for a registered cash register"
            )
        )
        assertTrue(locked.isNotEmpty() && refused.isNotEmpty())
    }

    private companion object {
        /** Колонка карточки кассы в рабочем месте владельца. */
        const val CARD_WIDTH = 760
        const val CARD_HEIGHT = 820
    }
}
