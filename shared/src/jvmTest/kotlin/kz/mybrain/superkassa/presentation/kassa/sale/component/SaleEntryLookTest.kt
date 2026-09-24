package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.entry.PositionEntryCard
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ввод позиции, скидка строки и данные покупателя — карточки кассовой колонки.
 *
 * Снимки — `/tmp/kassa-sale-*.png`: смотрят на поля, их шаг и молчание
 * нетронутой формы.
 */
class SaleEntryLookTest {

    /**
     * Нетронутая форма позиции молчит.
     *
     * При открытии смены, до первого товара, под погашенной кнопкой
     * «Добавить» стояло красное «Введите наименование товара»: касса
     * упрекала кассира за работу, которую он ещё не начинал, тогда как
     * сами поля формы в этот момент молчали. Ищется не надпись, а цвет:
     * красного в нетронутой форме быть не должно вовсе.
     */
    @Test
    fun `нетронутая форма позиции не краснеет`() {
        val fresh = KassaScene.shot("audit-sale-entry-fresh", width = ENTRY_WIDE, height = ENTRY_TALL) {
            Entry(open())
        }

        assertEquals(0, redPixels(fresh), "нетронутая форма позиции показывает упрёк красным")
    }

    /**
     * Карточка ввода позиции — та же, что стоит в кассовой колонке.
     *
     * Черновик — часть состояния продажи: снимок подставляет его набранным.
     */
    @Composable
    private fun Entry(state: SaleUiState) {
        Till(state) {
            Column(modifier = Modifier.width(TILL).padding(Spacing.fieldGap)) {
                PositionEntryCard(state, NO_ENTRY, expanded = true, onToggle = {})
            }
        }
    }

    /**
     * Скидка позиции набирается тенге и долей — одним полем.
     *
     * Один кадр на оба способа: слева набранная сумма, справа доля
     * от стоимости строки. Под полем в обоих случаях стоит то же число
     * другим способом — кассир не пересчитывает его в уме.
     */
    @Test
    fun `скидка позиции набирается и суммой, и долей`() {
        val frame = KassaScene.shot("sale-trim-position-discount", width = PAIR_WIDE, height = ENTRY_TALL) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sectionGap)) {
                Entry(open().copy(draft = DISCOUNT_TENGE))
                Entry(open().copy(draft = DISCOUNT_PERCENT))
            }
        }
        assertTrue(frame.isNotEmpty())
    }

    /**
     * Скидки и наценки рядом с соседними карточками.
     *
     * Один кадр на три карточки кассовой колонки: поля внутри каждой
     * обязаны отстоять друг от друга на один и тот же шаг. Прежде поля
     * скидки и наценки разносило шире остальных, и разнобой был виден
     * с первого взгляда на колонку.
     */
    @Test
    fun `шаг полей в карточке скидок тот же, что у соседних`() {
        val frame = KassaScene.shot("sale-trim-changes-neighbours", width = ENTRY_WIDE, height = COLUMN_TALL) {
            Neighbours(open(POSITIONS.fold(Basket(), Basket::add)))
        }
        assertTrue(frame.isNotEmpty())
    }

    /** Три соседние карточки колонки — те же, что стоят в окне. */
    @Composable
    private fun Neighbours(state: SaleUiState) {
        Till(state) {
            Column(
                modifier = Modifier.width(TILL).padding(Spacing.fieldGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
            ) {
                PositionEntryCard(state, NO_ENTRY, expanded = true, onToggle = {})
                ReceiptChangesCard(state, NO_FORM, expanded = true, onToggle = {})
                PaymentCard(state, NO_PAYMENTS, expanded = true, onToggle = {})
                ReceiptTotals(state.form, state.total, expanded = true, onTaken = {})
            }
        }
    }

    /**
     * В данных покупателя стоит только он сам.
     *
     * Отраслевые поля — вид отрасли, лицевой счёт, номер машины, часы
     * стоянки — с экрана убраны, и блок обязан остаться тем, чем назван:
     * контакт, по которому покупателю уходит чек, и ИИН или БИН того, кому
     * он выписан. Под полем контакта — зачем оно, как набрать или куда уйдёт чек.
     */
    @Test
    fun `в данных покупателя — контакт для чека и ИИН или БИН`() {
        val typed = listOf("empty" to "", "wrong" to "8 701 000", "phone" to "8 (701) 765-43-21")
        val frames = typed.map { (name, text) ->
            KassaScene.shot("sale-customer-contact-$name", width = ENTRY_WIDE, height = ENTRY_TALL) {
                open().withChannels(PHONE_READY).chooseContact(ContactKind.Phone)
                    .let { Customer(it.copy(form = it.form.enterContact(text))) }
            }
        }
        assertTrue(frames.all { it.isNotEmpty() })
        val alike = frames.zipWithNext().any { (a, b) -> a.contentEquals(b) }
        assertTrue(!alike, "пустой, неверный и верный контакт неразличимы")
    }

    /** Блок данных покупателя — тот же, что стоит в кассовой колонке. */
    @Composable
    private fun Customer(state: SaleUiState) {
        Till(state) {
            Column(modifier = Modifier.width(TILL).padding(Spacing.fieldGap)) {
                CustomerDataCard(state.form, state.channels, NO_FORM, expanded = true, onToggle = {})
            }
        }
    }
}

/** Настроен только телефон: поле контакта стоит, почта и Telegram погашены. */
private val PHONE_READY = ContactChannels(setOf(ContactKind.Phone))
