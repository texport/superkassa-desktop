package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.adaptive.MoneyText
import kz.mybrain.superkassa.presentation.adaptive.NumberText
import kz.mybrain.superkassa.presentation.components.Chip
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.history.JournalDelivery
import kz.mybrain.superkassa.presentation.history.JournalEntry
import kz.mybrain.superkassa.presentation.history.JournalRow
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.englishStatus
import kz.mybrain.superkassa.presentation.strings.kazakhStatus
import kz.mybrain.superkassa.presentation.strings.russianStatus
import kz.mybrain.superkassa.presentation.theme.HistoryLayout
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TableColumns
import kz.mybrain.superkassa.presentation.theme.TextScale
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Столбцы журнала вмещают своё содержимое на обычной и крупной ступени.
 *
 * Плашка «Внутренний» рвалась на «Внутренни / й», «Қабылданбады» —
 * посреди слова; сумма в миллиарды и признак в десять цифр
 * наезжали на соседние столбцы. Меряется наименьшая ширина содержимого
 * против ширины столбца за вычетом полей ячейки, и высота строки.
 */
class HistoryColumnsTest {

    private val scales = listOf(TextScale.Normal, TextScale.Larger)

    /** Ширина содержимого столбца: наименьшая ширина, выросшая со шрифтом, без полей ячейки. */
    private fun room(min: Dp, scale: TextScale): Int =
        (min * scale.factor.coerceAtLeast(1f) - TableColumns.cellPadding * 2).value.toInt()

    /** Наименьшая ширина, которую просит содержимое. */
    private fun least(scale: TextScale, language: Language = Language.Ru, content: @Composable () -> Unit): Int {
        var width = 0
        RenderProbe(width = WIDE, height = TALL, look = Look(textScale = scale), language = language) {
            Box(modifier = Modifier.width(IntrinsicSize.Min).onGloballyPositioned { width = it.size.width }) {
                content()
            }
        }.use { it.frame() }
        return width
    }

    @Test
    fun `сумма, номер, признак и время помещаются в свои столбцы`() {
        scales.forEach { scale ->
            val money = least(scale) { MoneyText(Money.formatTiyn(BILLIONS)) }
            val sign = least(scale) { NumberText(SIGN) }
            val number = least(scale) { NumberText(NUMBER) }
            val moment = least(scale) { NumberText(MOMENT) }
            println("$scale: сумма $money, признак $sign, номер $number, время $moment")
            assertTrue(money <= room(TableColumns.money, scale), "сумма шире столбца на $scale")
            assertTrue(sign <= room(TableColumns.number, scale), "признак шире столбца на $scale")
            assertTrue(number <= room(TableColumns.number, scale), "номер шире столбца на $scale")
            assertTrue(moment <= room(TableColumns.moment, scale), "время шире столбца на $scale")
        }
    }

    @Test
    fun `плашка состояния не рвёт слово ни на одном языке`() {
        Language.entries.forEach { language ->
            scales.forEach { scale ->
                val words = JournalDelivery.entries.flatMap { it.title(STATUS.getValue(language)).split(' ') }
                words.forEach { word ->
                    val wide = least(scale, language) { Chip(word, Color.Gray, LABEL()) }
                    assertTrue(
                        wide <= room(HistoryLayout.deliveryStatus, scale),
                        "«$word» ($language, $scale) шире столбца состояния: $wide"
                    )
                }
            }
        }
    }

    @Test
    fun `строка журнала одной высоты при любом состоянии на крупной ступени`() {
        val heights = JournalDelivery.entries.map { state ->
            var height = 0
            val look = Look(textScale = TextScale.Larger)
            RenderProbe(width = WIDE, height = TALL, look = look, language = Language.Kk) {
                Column {
                    Box(modifier = Modifier.onGloballyPositioned { height = it.size.height }) {
                        JournalRow(entry = entry(state), striped = true, onPreview = {}, onPrint = {})
                    }
                }
            }.use { it.frame() }
            height
        }
        println("высоты строк журнала: $heights")
        heights.forEach { assertEquals(HistoryLayout.row.value.toInt(), it, "строка выросла: $heights") }
    }

    @Composable
    private fun LABEL() = MaterialTheme.typography.labelSmall

    /** Строка с предельными номером, суммой и признаком. */
    private fun entry(state: JournalDelivery) = JournalEntry(
        key = "row",
        at = 0,
        moment = MOMENT,
        typeCode = "RETURN",
        type = "Сатуды қайтару",
        number = NUMBER,
        numberOrder = 0,
        amount = Money.formatTiyn(BILLIONS),
        amountOrder = BigDecimal.ZERO,
        sign = SIGN,
        delivery = state,
        shiftNo = SHIFT
    )

    private companion object {
        const val WIDE = 1600
        const val TALL = 200
        const val BILLIONS = 999_999_999_999L
        /** Признак по спецификации — uint32: не длиннее десяти цифр. */
        const val SIGN = "4294967295"
        const val NUMBER = "9999999999"
        const val MOMENT = "23.09 17:34:19"
        const val SHIFT = 65_535L

        /** Названия состояний на каждом языке. */
        val STATUS = mapOf(Language.Ru to russianStatus, Language.Kk to kazakhStatus, Language.En to englishStatus)
    }
}
