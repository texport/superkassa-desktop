package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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
 *
 * Стоимость раскрытия считается строками, которые список собрал, а не
 * секундомером: на общей машине проверки время скачет, и сравнение
 * миллисекунд падало там, где список был в порядке.
 */
class PickerListTest {

    @Composable
    private fun Picker(count: Int, composed: IntArray = IntArray(1)) {
        val options = (1..count).map { "Торговая точка $it" }
        Box(modifier = Modifier.fillMaxSize().padding(Spacing.blockPadding)) {
            LabelledPicker(
                label = "Торговая точка",
                options = options,
                selected = options.firstOrNull(),
                title = {
                    composed[0]++
                    it.orEmpty()
                },
                onSelect = {}
            )
        }
    }

    /** Сколько строк список собрал, раскрываясь: нажатие по полю и кадры до конца хода. */
    private fun openedRows(count: Int): Int {
        val composed = IntArray(1)
        return RenderProbe { Picker(count, composed) }.use { probe ->
            probe.frame()
            composed[0] = 0
            probe.click(FIELD)
            composed[0]
        }
    }

    @Test
    fun `раскрытие длинного списка стоит столько же, сколько короткого`() {
        val short = openedRows(SHORT)
        val long = openedRows(LONG)
        println("раскрытие собрало строк: из $SHORT — $short, из $LONG — $long")
        assertTrue(long <= short * FACTOR, "раскрытие растёт с длиной списка: $short → $long строк")
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
            Language.entries.all { textsOf(it).common.general.nothingToPick.isNotBlank() },
            "надпись о пустом списке есть не на всех языках"
        )
    }

    private companion object {
        /** Куда нажать, чтобы раскрыть список: поле стоит первым в окне. */
        val FIELD = Offset(300f, 50f)

        const val SHORT = 20
        const val LONG = 2000

        /**
         * Во сколько раз длинный список вправе собрать больше строк, чем
         * короткий: видимых строк у обоих поровну, а собранный целиком
         * длинный список собрал бы в сто раз больше.
         */
        const val FACTOR = 2

        const val EMPTY_SHOT = "/tmp/picker-empty.png"
    }
}
