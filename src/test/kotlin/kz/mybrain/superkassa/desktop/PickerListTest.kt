package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.rowsHeight
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Раскрытый список выбора на длинном наборе и на пустом.
 *
 * В кабинете сети торговых точек и касс по две тысячи. Столбец, собранный
 * целиком, складывал их все на каждое раскрытие: раскрытие двух тысяч
 * строк стоило втрое дороже, чем двадцати, — 657 мс против 269 мс
 * на этой машине. Пустой набор раскрывался пустой рамкой, и та читалась
 * как сбой приложения.
 */
class PickerListTest {

    @Composable
    private fun Picker(count: Int) {
        val options = (1..count).map { "Торговая точка $it" }
        Box(modifier = Modifier.fillMaxSize().padding(Spacing.roomy)) {
            LabelledPicker(
                label = "Торговая точка",
                options = options,
                selected = options.firstOrNull(),
                title = { it.orEmpty() },
                onSelect = {}
            )
        }
    }

    /** Сколько стоит раскрыть список: нажатие по полю и кадры до конца хода. */
    private fun openMillis(count: Int): Long = RenderProbe { Picker(count) }.use { probe ->
        probe.frame()
        val started = System.nanoTime()
        probe.click(FIELD)
        (System.nanoTime() - started) / 1_000_000
    }

    @Test
    fun `раскрытие длинного списка стоит столько же, сколько короткого`() {
        openMillis(SHORT)
        val short = openMillis(SHORT)
        val long = openMillis(LONG)
        println("раскрытие: $SHORT строк — $short мс, $LONG строк — $long мс")
        assertTrue(long < short * FACTOR + SLACK, "рост раскрытия с длиной списка: $short → $long мс")
    }

    /**
     * Высота списка — по числу строк, но не выше предела: короткий набор
     * не должен занимать пол-окна, а длинный — расти без края.
     */
    @Test
    fun `высота списка идёт по числу строк до предела`() {
        assertEquals(Sizes.pickerRow * 2, rowsHeight(2))
        assertEquals(Sizes.pickerList, rowsHeight(LONG))
    }

    @Test
    fun `пустой список говорит словами, а не пустой рамкой`() {
        RenderProbe { Picker(0) }.use { probe ->
            val closed = probe.frame()
            probe.click(FIELD)
            val opened = probe.frame()
            File(EMPTY_SHOT).writeBytes(opened)
            assertTrue(!opened.contentEquals(closed), "пустой список раскрылся ничем")
        }
        assertTrue(
            Language.entries.all { stringsOf(it).common.nothingToPick.isNotBlank() },
            "надпись о пустом списке есть не на всех языках"
        )
    }

    private companion object {
        /** Куда нажать, чтобы раскрыть список: поле стоит первым в окне. */
        val FIELD = Offset(300f, 50f)

        const val SHORT = 20
        const val LONG = 2000

        /** Во сколько раз длинный список вправе оказаться дороже короткого. */
        const val FACTOR = 2

        /** Запас на разогрев машины, не зависящий от длины списка. */
        const val SLACK = 150L

        const val EMPTY_SHOT = "/tmp/picker-empty.png"
    }
}
