package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.sale.LocalSaleTexts
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.PriceAskDialog
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import java.io.File
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Окно цены для позиции каталога без цены.
 *
 * Снимки — `/tmp/price-ask-*.png`, и смотрит их человек: видно ли, что
 * именно добавляют, и названа ли причина отказа у своего поля. Работа
 * с клавиатуры проверяется тут же: за кассой руки от неё не отпускают,
 * и окно, которое закрывается только мышью, останавливает очередь.
 */
class PriceAskShots {

    private val units = listOf(
        UnitOfMeasurement(code = "796", nameShort = "шт", nameFull = "Штука"),
        UnitOfMeasurement(code = "166", nameShort = "кг", nameFull = "Килограмм")
    )

    /** Карточка НТИН без цены: каталог описывает товар, цену назначает продавец. */
    private val weighed = Position(
        name = "Сыр полутвёрдый «Қазақ»",
        price = BigDecimal.ZERO,
        quantity = BigDecimal.ONE,
        vatGroup = "VAT_16",
        measureUnitCode = "166",
        ntin = "KZ01234567890123"
    )

    @Test
    fun `окно спрашивает цену и называет отказ у своего поля`() {
        val empty = shot("price-ask-empty", weighed)
        val typed = shot("price-ask-typed", weighed) { probe -> probe.type("2500") }
        // Дробное количество у штуки: причина стоит под количеством,
        // а кнопка добавления погашена.
        val refused = shot("price-ask-fraction", weighed.copy(measureUnitCode = "796")) { probe ->
            probe.type("2500")
            probe.key(Key.Tab)
            probe.type("1,45")
        }

        val frames = listOf(empty, typed, refused)
        assertTrue(frames.map { it.toList() }.distinct().size == frames.size, "состояния окна неотличимы")
    }

    @Test
    fun `Enter добавляет позицию с заданной ценой`() {
        val added = mutableListOf<Position>()
        scene(weighed, added) { probe ->
            probe.type("2500")
            probe.key(Key.Enter)
        }

        assertEquals(1, added.size)
        assertEquals(0, BigDecimal("2500").compareTo(added.single().price))
        assertEquals("KZ01234567890123", added.single().ntin)
    }

    /**
     * Tab переводит к количеству, и набранное затирает подставленную
     * единицу: «3» поверх неё давало тридцать одну штуку.
     */
    @Test
    fun `количество набирается поверх подставленной единицы`() {
        val added = mutableListOf<Position>()
        scene(weighed, added) { probe ->
            probe.type("2500")
            probe.key(Key.Tab)
            probe.type("3")
            probe.key(Key.Enter)
        }

        assertEquals(0, BigDecimal("3").compareTo(added.single().quantity))
    }

    @Test
    fun `Escape закрывает окно и ничего не добавляет`() {
        val added = mutableListOf<Position>()
        val closed = scene(weighed, added) { probe ->
            probe.type("2500")
            probe.key(Key.Escape)
        }

        assertTrue(closed(), "окно не закрылось по Escape")
        assertTrue(added.isEmpty(), "отказ добавил позицию в чек")
    }

    /** Кадр окна в файл: состояние набирается теми же нажатиями, что у кассира. */
    private fun shot(name: String, found: Position, act: (RenderProbe) -> Unit = {}): ByteArray {
        var frame = ByteArray(0)
        probe(found, mutableListOf(), onDismiss = {}) { scene ->
            act(scene)
            frame = scene.frame()
        }
        File("/tmp/$name.png").writeBytes(frame)
        assertTrue(frame.isNotEmpty(), "снимок $name пуст")
        return frame
    }

    /** Сцена с окном: возвращает признак того, что окно попросили закрыть. */
    private fun scene(
        found: Position,
        added: MutableList<Position>,
        act: (RenderProbe) -> Unit
    ): () -> Boolean {
        var dismissed = false
        probe(found, added, onDismiss = { dismissed = true }, act = act)
        return { dismissed }
    }

    private fun probe(
        found: Position,
        added: MutableList<Position>,
        onDismiss: () -> Unit,
        act: (RenderProbe) -> Unit
    ) {
        RenderProbe {
            Dialog(found, onAdd = { added.add(it) }, onDismiss = onDismiss)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            act(probe)
        }
    }

    /** Надписи области подставляются те же, что на экране продажи. */
    @Composable
    private fun Dialog(found: Position, onAdd: (Position) -> Unit, onDismiss: () -> Unit) {
        CompositionLocalProvider(LocalSaleTexts provides saleTexts(Language.Ru)) {
            PriceAskDialog(found, units, onAdd, onDismiss)
        }
    }

    private companion object {
        /** Сколько кадров даётся окну, чтобы открыться и взять фокус. */
        const val SETTLE = 20
    }
}
