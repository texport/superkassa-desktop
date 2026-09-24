package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.presentation.journal.HistoryContent
import kz.mybrain.superkassa.presentation.journal.HistoryParts
import kz.mybrain.superkassa.presentation.journal.HistoryStage
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Окно доставки чека поверх журнала — во всех окнах, на трёх языках,
 * в светлом и тёмном оформлении и на крупной ступени шрифта.
 *
 * Данные предельные: причина отказа на три строки, код отказа (он не показывается), все
 * четыре канала. Кадры — `/tmp/qa-journal-delivery-<состояние>-<окно>-<язык>-<ступень>-<оформление>.png`.
 */
class ReceiptDeliveryShots {

    private fun shot(
        name: String,
        width: Int,
        height: Int,
        mode: HistoryStage.Mode,
        delivery: ReceiptDeliveryUi
    ): List<ProbeNode> {
        val journal = JournalUiState(
            documents = HistoryStage.documents(ROWS),
            page = PageOutcome.page(more = false),
            loading = false,
            delivery = delivery
        )
        val desk = KassaScene.desk()
        val look = Look(textScale = mode.scale)
        return RenderProbe(width, height, mode.appearance, look, mode.language) {
            HistoryStage.Window(desk, Section.History, HistoryStage.Place()) { HistoryContent(HistoryParts(journal)) }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/qa-journal-delivery-$name-${width}x$height-${mode.tag}.png").writeBytes(probe.frame())
            probe.nodes()
        }
    }

    @Test
    fun `повтор виден и в окне в каждом окне, на каждом языке и в обоих оформлениях`() {
        HistoryStage.SIZES.forEach { (width, height) ->
            MODES.forEach { mode ->
                val nodes = shot("failed", width, height, mode, failed())
                val resend = textsOf(mode.language).journal.delivery.resend
                val where = "${mode.tag} $width×$height"
                val button = assertNotNull(nodes.firstOrNull { it.text == resend }, "нет кнопки повтора: $where")
                assertTrue(button.whole, "кнопка повтора обрезана: ${mode.tag} $width×$height — $button")
                assertTrue(
                    button.visible.right <= width && button.visible.bottom <= height,
                    "кнопка повтора за краем окна: ${mode.tag} $width×$height — $button"
                )
            }
        }
    }

    @Test
    fun `пусто, ждёт БФД, читается и отказ кассы`() {
        val states = mapOf(
            "empty" to ReceiptDeliveryUi(DOCUMENT, NUMBER, awaitingBfd = false, reading = false),
            "awaiting" to ReceiptDeliveryUi(DOCUMENT, NUMBER, awaitingBfd = true, reading = false),
            "reading" to ReceiptDeliveryUi(DOCUMENT, NUMBER, awaitingBfd = false),
            "refused" to ReceiptDeliveryUi(DOCUMENT, NUMBER, awaitingBfd = false, reading = false, problem = REFUSAL)
        )
        states.forEach { (name, state) ->
            listOf(SMALLEST, TABLET).forEach { (width, height) ->
                val nodes = shot(name, width, height, HistoryStage.Mode(Language.Kk, TextScale.Larger), state)
                val close = textsOf(Language.Kk).journal.delivery.close
                assertTrue(nodes.any { it.text == close && it.whole }, "нет кнопки «Жабу»: $name $width×$height")
                val resend = textsOf(Language.Kk).journal.delivery.resend
                assertTrue(nodes.none { it.text == resend }, "повтор предложен без отказа: $name")
            }
        }
    }

    private fun failed() = ReceiptDeliveryUi(
        documentId = DOCUMENT,
        number = NUMBER,
        awaitingBfd = false,
        reading = false,
        deliveries = listOf(
            channel("SMS", "LINK", ReceiptDeliveryState.FAILED),
            channel("TELEGRAM", "PDF", ReceiptDeliveryState.PENDING),
            channel("WHATSAPP", "IMAGE", ReceiptDeliveryState.DELIVERED),
            channel("EMAIL", "HTML", ReceiptDeliveryState.FAILED)
        )
    )

    private fun channel(code: String, payload: String, state: ReceiptDeliveryState): ReceiptDeliveryResponse {
        val failed = state != ReceiptDeliveryState.DELIVERED
        return ReceiptDeliveryResponse(
            channel = code,
            payloadType = payload,
            state = state,
            attempts = if (failed) ATTEMPTS else 1,
            nextAttemptAt = if (state == ReceiptDeliveryState.PENDING) System.currentTimeMillis() + MINUTE else null,
            failureCode = if (failed) "DELIVERY_${code}_PROVIDER_REFUSED" else null,
            failureMessage = if (failed) TrilingualMessageResponse(REFUSAL, REFUSAL_KK, REFUSAL_EN) else null,
            updatedAt = System.currentTimeMillis()
        )
    }

    private companion object {
        const val SETTLE = 20
        const val ROWS = 40
        const val ATTEMPTS = 5
        const val MINUTE = 60_000L
        const val DOCUMENT = "doc-1"
        const val NUMBER = "9223372036854"
        val SMALLEST = 960 to 640
        val TABLET = 800 to 1280
        val MODES = listOf(
            HistoryStage.Mode(),
            HistoryStage.Mode(Language.Kk, TextScale.Larger),
            HistoryStage.Mode(Language.En, TextScale.Normal, Appearance.Dark),
            HistoryStage.Mode(Language.Ru, TextScale.Larger, Appearance.Dark)
        )
        const val REFUSAL = "Провайдер SMS отклонил сообщение: номер получателя не обслуживается, " +
            "проверьте номер покупателя или выберите другой канал доставки"
        const val REFUSAL_KK = "SMS провайдері хабарламаны қабылдамады: алушының нөмірі қызмет көрсетілмейді, " +
            "сатып алушының нөмірін тексеріңіз немесе басқа жеткізу арнасын таңдаңыз"
        const val REFUSAL_EN = "The SMS provider refused the message: the recipient number is not served, " +
            "check the customer's number or choose another delivery channel"
    }
}
