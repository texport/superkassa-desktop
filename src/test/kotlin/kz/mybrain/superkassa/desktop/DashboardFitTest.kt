package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.dashboard.DashboardScreen
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Главный экран в самом тесном окне, за которым работают.
 *
 * Окно 1000×700 и самая крупная ступень шрифта — ноутбук кассира и экран
 * прилавка. Карточки над списком росли вниз без предела: перечень
 * отклонённых БФД документов приходит пачкой, и он выдавливал за нижний
 * край и заголовок «Документы смены», и сам список — кассир не видел
 * ни одного чека своей смены и не мог до них добраться.
 *
 * Кадры остаются в `/tmp/dashboard-fit-*.png`.
 */
class DashboardFitTest {

    private fun sale(no: Long, refused: Boolean) = Document(
        id = "doc-$no",
        docNo = no,
        printedDocumentNumber = no,
        docType = "SALE",
        ofdStatus = if (refused) "FAILED" else "SENT",
        ofdErrorCode = if (refused) 1015 else null,
        ofdErrorText = if (refused) "Same customer and taxpayer IIN" else null,
        totalAmount = no * 120_000,
        createdAt = System.currentTimeMillis(),
        shiftNo = 7
    )

    private fun busyShift(folder: String): Session = KassaScene.session(
        folder,
        shift = KassaScene.openShift(),
        documents = (1L..40L).map { sale(it, refused = it <= REFUSED) }
    )

    @Composable
    private fun Work(session: Session) {
        Row(modifier = Modifier.fillMaxSize()) {
            SectionRail(Section.entries, Section.Dashboard, false, {}, { Text(VERSION) }) {}
            DashboardScreen(session)
        }
    }

    private fun scrolls(name: String, scale: TextScale, at: Offset): Boolean {
        val session = busyShift("fit-${scale.code}")
        return RenderProbe(
            width = WIDTH,
            height = HEIGHT,
            look = Look(textScale = scale)
        ) { Work(session) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            File("/tmp/dashboard-fit-$name.png").writeBytes(before)
            probe.wheel(at = at, ticks = WHEEL)
            probe.changedFrom(before)
        }
    }

    @Test
    fun `в низком окне документы смены видны и прокручиваются`() {
        TextScale.entries.forEach { scale ->
            assertTrue(
                scrolls("documents-${scale.code}", scale, Offset(DOCUMENTS_X, DOCUMENTS_Y)),
                "при ступени $scale список документов смены не достаётся кассиру в окне 1000×700"
            )
        }
    }

    /** Пачка отказов прокручивается внутри своей карточки, а не растёт вниз. */
    @Test
    fun `перечень отказов прокручивается внутри карточки`() {
        assertTrue(
            scrolls("refused", TextScale.Normal, Offset(DOCUMENTS_X, REFUSED_Y)),
            "перечень отклонённых документов не прокручивается"
        )
    }

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 700
        const val SETTLE = 40
        const val WHEEL = 6f
        const val VERSION = "1.0.0"

        /** Сколько документов смены БФД отверг: отказы приходят пачкой. */
        const val REFUSED = 12L

        /** Где на экране лежит список документов смены и где — карточка отказов. */
        const val DOCUMENTS_X = 600f
        const val DOCUMENTS_Y = 620f
        const val REFUSED_Y = 300f
    }
}
