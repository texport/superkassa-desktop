package kz.mybrain.superkassa.domain.update.usecase

import kz.mybrain.superkassa.presentation.settings.MemoryUpdates
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Проверка выпусков по расписанию: раз в сутки от записанной проверки. */
class ReadUpdateScheduleTest {

    private val now = Instant.parse("2026-09-24T10:00:00Z")

    @Test
    fun `ни разу не проверялось — пора`() {
        assertTrue(ReadUpdateSchedule(MemoryUpdates(lastChecked = null)) { now }().due)
    }

    @Test
    fun `проверялось меньше суток назад — рано`() {
        val memory = MemoryUpdates(lastChecked = now - 23.hours)

        assertFalse(ReadUpdateSchedule(memory) { now }().due)
    }

    @Test
    fun `прошли сутки — пора`() {
        val memory = MemoryUpdates(lastChecked = now - 24.hours)

        assertTrue(ReadUpdateSchedule(memory) { now }().due)
    }

    @Test
    fun `выключенная проверка не наступает никогда`() {
        val memory = MemoryUpdates(automatic = true, lastChecked = null)
        SwitchAutomaticChecks(memory)(false)

        assertFalse(ReadUpdateSchedule(memory) { now }().due)
    }
}
