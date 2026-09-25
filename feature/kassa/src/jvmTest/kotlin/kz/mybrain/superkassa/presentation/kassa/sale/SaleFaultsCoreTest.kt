package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.string.api.CoreStrings
import kotlinx.coroutines.Dispatchers
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LosingKassa
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Продажа, когда что-то идёт не так: нет связи, БФД отказал, ответ
 * потерялся, сутки смены прошли. Итог сверяется с ядром и с БФД:
 * ни одного лишнего чека и ни одного потерянного.
 */
class SaleFaultsCoreTest {
    private val desk = CoreDesk()
    private val texts = textsOf(Language.Ru).common

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `нет связи — чек пробит автономно, кассиру сказано, что он ждёт отправки`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Хлеб", "450")
        desk.bfd.disconnect()

        model.issue()

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertTrue(texts.general.queuedNoLink in desk.saidText, "не сказано, что чек ждёт связи: ${desk.saidText}")
        assertTrue(model.state.value.basket.positions.isEmpty())
        assertTrue(kassa.sales().single().isAutonomous, "документ не автономный")
        assertTrue(desk.bfd.countedTickets().isEmpty())
    }

    @Test
    fun `БФД отказал — кассир видит отказ с причиной, чек остаётся и пробивается снова новым ключом`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Хлеб", "450")
        val key = model.state.value.attemptKey
        desk.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        model.issue()

        val refusal = assertIs<Message.Refusal>(desk.said, "отказ БФД показан как успех: ${desk.saidText}")
        assertEquals(INCORRECT_REQUEST_DATA.toString(), refusal.code)
        assertTrue(texts.status.refused in refusal.text, refusal.text)
        // Причина — словами кассы о коде БФД, а не одно «Отклонён».
        val words = CoreStrings.bfdRefusal(INCORRECT_REQUEST_DATA)
        assertTrue(listOf(words.ru, words.kk, words.en).any { it in refusal.text }, "нет причины: ${refusal.text}")
        assertEquals(1, model.state.value.basket.positions.size, "отвергнутый чек пропал с экрана")
        assertTrue(model.state.value.attemptKey != key, "повтор ушёл бы с ключом отвергнутого чека")
        model.issue()
        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(1, desk.bfd.countedTickets().size)
        assertEquals(listOf("FAILED", "SENT"), kassa.sales().map { it.ofdStatus }.sortedBy { it })
    }

    @Test
    fun `ответ БФД потерян — чек проходит один раз, и повтор не пробивает второй`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Хлеб", "450")
        desk.bfd.loseNextAnswer()

        model.issue()
        kassa.resendQueue()

        assertEquals(1, kassa.sales().size, "в ядре два чека на одну продажу")
        assertEquals(1, desk.bfd.countedTickets().size, "БФД учёл продажу дважды или не учёл вовсе")
    }

    @Test
    fun `ответ кассы не дошёл до экрана — повтор тем же ключом не пробивает второй чек`() {
        val kassa = desk.seated()
        val losing = LosingKassa(EmbeddedKassa(desk.bench.api, Dispatchers.Unconfined))
        val services = CoreScene.services(losing, desk.signIn, desk.notices)
        val model = saleModel(services, KassaPorts(FixedDeliverySetup())).also { it.visit() }
        model.add("Хлеб", "450")
        val key = model.state.value.attemptKey

        model.issue()
        assertIs<Message.NoAnswer>(desk.said, desk.saidText)
        assertEquals(key, model.state.value.attemptKey, "после неизвестного исхода ключ сменился")
        model.issue()

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(1, kassa.sales().size, "в ядре два чека на одну продажу")
        assertEquals(1, desk.bfd.countedTickets().size)
    }

    @Test
    fun `кассир ушёл в другой раздел и вернулся — корзина и ключ попытки на месте`() {
        desk.seated()
        val model = desk.sale()
        model.add("Хлеб", "450")
        model.form.taken("1000")
        val before = model.state.value

        model.visit()

        assertEquals(before.basket, model.state.value.basket)
        assertEquals(before.form, model.state.value.form)
        assertEquals(before.attemptKey, model.state.value.attemptKey)
    }

    @Test
    fun `сутки смены прошли — касса отказывает в чеке своим кодом, корзина остаётся`() {
        val kassa = desk.seated()
        kassa.sell()
        val model = desk.sale()
        model.add("Хлеб", "450")
        kassa.clock.move(DAY_AND_MINUTE)

        model.issue()

        val refusal = assertIs<Message.Refusal>(desk.said, desk.saidText)
        assertTrue(refusal.text.isNotBlank())
        assertEquals(1, model.state.value.basket.positions.size, "чек пропал после отказа")
        assertEquals(1, kassa.sales().size)
    }

    private companion object {
        /** RESULT_TYPE_INCORRECT_REQUEST_DATA: БФД не принял данные документа. */
        const val INCORRECT_REQUEST_DATA = 13
        const val DAY_AND_MINUTE = 24L * 60 * 60 * 1000 + 60_000
    }
}
