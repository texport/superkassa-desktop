package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.scan.CameraAccess
import kz.mybrain.superkassa.presentation.common.scan.CodeCamera
import kz.mybrain.superkassa.presentation.common.scan.LocalCodeCamera
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.measureUnits
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Третий способ набрать штрихкод — камера устройства.
 *
 * Владелец: «помимо клавиатуры и цифр — камера, и сканер штрихкодов».
 * Значок камеры стоит там, где камера есть; разрешение объясняется
 * до вопроса системы; прочитанный код ложится в поле и ищется, как код
 * сканера с Enter. Камера здесь подставная: картинки нет, код она отдаёт сразу.
 */
class BarcodeCameraTest {
    private val scan = textsOf(Language.Ru).kassa.scan

    @Test
    fun `без камеры значка камеры нет`() {
        val labels = shot(camera = null).map { it.label }
        assertTrue(scan.scan !in labels, "на машине без камеры предложен сканер камерой")
    }

    @Test
    fun `без разрешения окно объясняет, зачем камера, и просит её`() {
        var asked = 0
        val camera = FakeCamera(CameraAccess.Missing { asked++ })
        RenderProbe(width = SIZE, height = SIZE) { Field(camera, Recorded()) }.use { probe ->
            settle(probe)
            probe.tap { it.label == scan.scan }
            settle(probe)
            assertTrue(probe.nodes().any { it.text == scan.why }, "окно не объяснило, зачем кассе камера")
            probe.tap { it.text == scan.allow }
            settle(probe)
            assertEquals(1, asked, "разрешение не попрошено")
        }
    }

    @Test
    fun `прочитанный код ложится в поле и ищется, окно закрывается`() {
        val actions = Recorded()
        RenderProbe(width = SIZE, height = SIZE) { Field(FakeCamera(CameraAccess.Granted), actions) }.use { probe ->
            settle(probe)
            probe.tap { it.label == scan.scan }
            settle(probe)
            assertEquals(listOf(CODE), actions.typed, "код камеры не попал в поле")
            assertEquals(1, actions.searched, "код камеры не ищется")
            assertTrue(probe.nodes().none { it.text == scan.title }, "окно сканера осталось открытым")
        }
    }

    private fun shot(camera: CodeCamera?) =
        RenderProbe(width = SIZE, height = SIZE) { Field(camera, Recorded()) }.use { probe ->
            settle(probe)
            probe.nodes()
        }

    private fun settle(probe: RenderProbe) = repeat(SETTLE) { probe.frame() }

    @Composable
    private fun Field(camera: CodeCamera?, actions: EntryActions) {
        val field = @Composable {
            CompositionLocalProvider(
                LocalSaleTexts provides textsOf(Language.Ru).kassa.sale,
                LocalVatRates provides SaleUiState().vat(Language.Ru, LocalStrings.current.enums),
                LocalUnits provides measureUnits(Language.Ru)
            ) {
                Box(Modifier.padding(Spacing.fieldGap)) { BarcodeField(SaleUiState(), actions) }
            }
        }
        if (camera == null) field() else CompositionLocalProvider(LocalCodeCamera provides camera) { field() }
    }

    /** Что поле сделало: набранное и сколько раз искали. */
    private class Recorded : EntryActions {
        val typed = mutableListOf<String>()
        var searched = 0

        override fun typeBarcode(text: String) {
            typed += text
        }

        override fun search(): Boolean {
            searched++
            return true
        }
    }

    /** Камера без картинки: доступ тот, что дали, код отдаёт сразу. */
    private class FakeCamera(private val access: CameraAccess) : CodeCamera {
        override val available: Boolean = true

        @Composable
        override fun access(): CameraAccess = access

        @Composable
        override fun Viewfinder(onCode: (String) -> Unit, modifier: Modifier) {
            LaunchedEffect(Unit) { onCode(CODE) }
        }
    }

    private companion object {
        const val SIZE = 720
        const val SETTLE = 20
        const val CODE = "4870204391510"
    }
}
