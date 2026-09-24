package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Условия настроек кассы, названные до нажатия: сверка с БФД и выбор НДС.
 *
 * Правило проверяется без экрана — экран только гасит кнопку по нему.
 */
class KkmSettingRulesTest {

    @Test
    fun `сверка ждёт пустой очереди, сведения о кассе — ещё и закрытой смены`() {
        val idle = CoreScene.kkm().copy(offlineQueueCount = 0, isShiftOpen = false)
        assertTrue(KkmSettingRules.syncable(idle))
        assertTrue(KkmSettingRules.serviceSyncable(idle))

        val queued = idle.copy(offlineQueueCount = 3)
        assertFalse(KkmSettingRules.syncable(queued), "чек в очереди ушёл бы в БФД после сверки")
        assertFalse(KkmSettingRules.serviceSyncable(queued))

        val open = idle.copy(isShiftOpen = true)
        assertTrue(KkmSettingRules.syncable(open))
        assertFalse(KkmSettingRules.serviceSyncable(open), "сведения о кассе сверяются при закрытой смене")
    }

    @Test
    fun `ставку НДС выбирает только плательщик НДС`() {
        assertFalse(KkmSettingRules.vatChoosable(TaxRegime.NO_VAT.name))
        assertTrue(KkmSettingRules.vatChoosable(TaxRegime.entries.first { it != TaxRegime.NO_VAT }.name))
        assertTrue(KkmSettingRules.vatChoosable(null), "режим ещё не выбран — ставка не заперта")
    }
}
