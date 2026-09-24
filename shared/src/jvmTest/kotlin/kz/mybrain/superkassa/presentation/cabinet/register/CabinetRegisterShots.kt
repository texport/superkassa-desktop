package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.WithCabinetMessage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CashRegisterModel
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.shot
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки паспорта кассы в кабинете.
 *
 * Техническое состояние проверено отдельно (`TechnicalStateRenderTest`),
 * здесь смотрят на другое: видно ли по паспорту, что касса не на учёте
 * или снята с него, и гаснет ли выдача токена там, где кабинет её
 * всё равно не даст.
 */
class CabinetRegisterShots {

    private fun stage() = CabinetStage { path ->
        when (path) {
            "/api/cash-registers", "/api/retail-places" ->
                StubReply("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
            else -> StubReply("{}")
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
        model = CashRegisterModel("0x0065000086cb", "«ПОРТ FPG-350 ФKZ»"),
        retailPlace = RetailPlaceRef("p-1", "Магазин на Абая"),
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
        here: KkmResponse? = null,
        problem: CabinetProblem? = null
    ): ByteArray {
        val stage = stage()
        val register = register(status, number)
        // Кассы этой машины — как их отдала касса процесса.
        val known = RegisterUiState(card = register, state = snapshot(), kkms = listOfNotNull(here), kkmsRead = true)
        return shot(name, width = CARD_WIDTH, height = CARD_HEIGHT) {
            val body = @Composable {
                stage.Window {
                    val model = registerViewModel(stage.cabinet.cabinet)
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                        val view = RegisterView(register, register, known)
                        RegisterPassport(stage.cabinet, stage.texts, view, model)
                    }
                }
            }
            if (problem == null) body() else WithCabinetMessage(problem) { body() }
        }
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
     * Касса машины знает только свои кассы, и сверка идёт по идентификатору
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
        CoreScene.kkm(state = state, name = "Касса у входа", id = "k-1").copy(ofdSystemId = "5000021")

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
