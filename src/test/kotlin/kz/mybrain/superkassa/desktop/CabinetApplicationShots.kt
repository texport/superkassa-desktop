package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.CloseShiftBeforeDeregister
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistrationActionsBlock
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistrationCardBlock
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки заявлений в КГД и регистрационной карты.
 *
 * Отказной путь здесь главный: ИСНА отвечает через десятки секунд и
 * отвечает кодом, а владелец должен прочесть причину под кнопкой,
 * не гадая. Отдельно смотрят на открытую смену — единственный отказ,
 * который приложение берётся исправить само.
 */
class CabinetApplicationShots {

    private fun stage(prepare: CabinetReply = CabinetReply("{}")) = CabinetStage { path ->
        when {
            path == "/api/retail-places" -> CabinetReply(CabinetBodies.PLACES)
            path.endsWith("/application") -> prepare
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
        registrationCardAvailable = status == "REGISTERED"
    )

    @Composable
    private fun Block(stage: CabinetStage, status: String, number: String? = "000000010001") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            RegistrationActionsBlock(stage.session, stage.cabinet, stage.texts, register(status, number)) {}
        }
    }

    @Test
    fun `по состоянию кассы предлагаются разные заявления, а где нечего — сказано почему`() {
        val frames = mapOf(
            "application-draft" to shot("application-draft", CARD_WIDTH, CARD_HEIGHT) {
                Block(stage(), "DRAFT", number = null)
            },
            "application-registered" to shot("application-registered", CARD_WIDTH, CARD_HEIGHT) {
                Block(stage(), "REGISTERED")
            },
            "application-in-isna" to shot("application-in-isna", CARD_WIDTH, CARD_HEIGHT) {
                Block(stage(), "REGISTRATION_IN_ISNA_PROCESS", number = null)
            },
            "application-deregistered" to shot("application-deregistered", CARD_WIDTH, CARD_HEIGHT) {
                Block(stage(), "DEREGISTERED")
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(frames.values.map { it.toList() }.distinct().size == frames.size, "состояния неотличимы")
    }

    /**
     * Отказ ИСНА остаётся под кнопкой.
     *
     * Подача останавливается на подготовке — до подписи, — поэтому
     * NCALayer в этом случае не спрашивается вовсе: кабинет отвергает
     * заявление раньше.
     */
    @Test
    fun `отказ по заявлению читается под кнопкой подачи`() {
        val stage = stage(refusal("OKED_REQUIRED", "Primary OKED is required for registration", HttpStatusCode.BadRequest))
        RenderProbe(width = CARD_WIDTH, height = CARD_HEIGHT, content = {
            Block(stage, "DRAFT", number = null)
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(SUBMIT_X, SUBMIT_Y))
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/cabinet-application-refused.png").writeBytes(frame)
            assertTrue(frame.isNotEmpty())
        }
    }

    /**
     * Смена не закрыта: кабинет отказывает снять кассу.
     *
     * У кассы этой машины предлагается закрыть смену здесь же, у чужой —
     * сказано, где её закрывать. Оба случая снимаются отдельно: разница
     * между ними и есть то, из-за чего владелец прежде ходил кругами.
     */
    @Test
    fun `открытая смена мешает снятию с учёта, и предложение зависит от машины`() {
        val here = shiftOpen("application-shift-here", node = Kkm(kkmId = "k-1", ofdSystemId = "5000021", state = "ACTIVE"))
        val elsewhere = shiftOpen("application-shift-elsewhere", node = null)

        assertTrue(here.isNotEmpty() && elsewhere.isNotEmpty())
        assertTrue(!here.contentEquals(elsewhere), "своя касса и чужая предлагают одно и то же")
    }

    private fun shiftOpen(name: String, node: Kkm?): ByteArray {
        val stage = stage(refusal("SHIFT_IS_OPEN", "Shift is open", HttpStatusCode.Conflict))
        node?.let { stage.session.kkms.add(it) }
        RenderProbe(width = CARD_WIDTH, height = CARD_HEIGHT, content = {
            Block(stage, "REGISTERED")
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(KIND_DEREGISTER_X, KIND_Y))
            probe.click(Offset(SUBMIT_X, DEREGISTER_SUBMIT_Y))
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/cabinet-$name.png").writeBytes(frame)
            return frame
        }
    }

    /** Окно закрытия смены с пином администратора. */
    @Test
    fun `окно закрытия смены спрашивает пин и называет причину`() {
        val stage = stage()
        val frame = shot("application-close-shift", CARD_WIDTH, CARD_HEIGHT) {
            CloseShiftBeforeDeregister(stage.texts, busy = false, onDismiss = {}) {}
        }
        assertTrue(frame.isNotEmpty())
    }

    @Test
    fun `регистрационной карты нет и карта с версиями`() {
        val missing = CabinetStage { CabinetReply("{}") }
        val absent = shot("card-missing", CARD_WIDTH, CARD_HEIGHT) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                RegistrationCardBlock(missing.cabinet, missing.texts, register("DRAFT", number = null))
            }
        }

        val issued = CabinetStage { path ->
            when {
                path.endsWith("/registration-card") -> CabinetReply(CabinetBodies.CARD)
                path.endsWith("/versions") -> CabinetReply(CabinetBodies.CARD_VERSIONS)
                else -> CabinetReply("{}")
            }
        }
        val present = shot("card-versions", CARD_WIDTH, CARD_HEIGHT) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                RegistrationCardBlock(issued.cabinet, issued.texts, register("REGISTERED"))
            }
        }

        assertTrue(!absent.contentEquals(present), "карта есть и карты нет выглядят одинаково")
    }

    private companion object {
        const val CARD_WIDTH = 760
        const val CARD_HEIGHT = 560
        const val SETTLE = 30

        /** Кнопка подачи у кассы-черновика: под единственным сегментом. */
        const val SUBMIT_X = 120f
        const val SUBMIT_Y = 74f

        /** Ряд сегментов: третий — снятие с учёта. */
        const val KIND_DEREGISTER_X = 550f
        const val KIND_Y = 36f

        /** Кнопка подачи при выбранном снятии: под причинами и полем пояснения. */
        const val DEREGISTER_SUBMIT_Y = 231f
    }
}
