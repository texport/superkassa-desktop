package kz.mybrain.superkassa.presentation.cabinet.register.adopt

import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.setup.model.OfdContours
import kz.mybrain.superkassa.kassa.MemorySetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Поставщик один, контур один поднят.
 *
 * Касса этого продукта заводится только в БФД, и отвечает у неё пока
 * только стенд разработки: выбор из одного значения владельцу решать
 * нечего, а чужой оператор или неподнятый контур кончались бы отказом
 * уже после нажатия.
 */
class BfdTargetTest {

    /** Те же контуры, как их отдаёт справочник кассы процесса. */
    private val kassaEnvironments = listOf("DEV", "TEST", "PROD").map {
        OfdEnvironmentResponse(code = it, name = TrilingualMessageResponse(it, it, it))
    }

    @Test
    fun `поставщик подставлен, а не пуст`() {
        assertEquals(BFD_PROVIDER, OfdTarget().provider)
    }

    /** Полнота считается по обоим полям: подставленный поставщик её не отменяет. */
    @Test
    fun `выбор полон, как только назван контур`() {
        assertFalse(OfdTarget().complete, "без контура заводить нечего")
        assertTrue(OfdTarget(environment = "DEV").complete)
    }

    @Test
    fun `поднят только стенд разработки`() {
        assertTrue(OfdContours.raised("DEV"))
        assertFalse(OfdContours.raised("TEST"))
        assertFalse(OfdContours.raised("PROD"))
    }

    @Test
    fun `окно заведения подставляет БФД и первый поднятый контур`() {
        val draft = AdoptDraft(MemorySetup())
        draft.preset(kassaEnvironments)
        assertEquals(BFD_PROVIDER, draft.target.provider)
        assertEquals("DEV", draft.target.environment)
    }

    /**
     * Рабочее место помнит контур, но не поставщика.
     *
     * Запомненный чужой оператор пережил бы скрытие выбора и ушёл бы
     * в кассу из настроек, которых владелец больше не видит.
     */
    @Test
    fun `запомненное рабочим местом поставщика не меняет`() {
        val shared = MemorySetup()
        val first = AdoptDraft(shared)
        first.target = first.target.copy(environment = "PROD")
        first.remember()

        val next = AdoptDraft(shared)
        next.preset(kassaEnvironments)
        assertEquals(BFD_PROVIDER, next.target.provider)
        assertEquals("PROD", next.target.environment, "контур рабочее место помнит")
    }
}
