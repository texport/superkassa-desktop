package kz.mybrain.superkassa.domain.analytics.model

import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/**
 * Сумма проверки в тиынах по записи тенге: `"61825.00"` → 6 182 500.
 *
 * Проверки пишут суммы так, как их пишет кабинет, а переводит запись
 * модуль кабинета — тем же правилом, что и живые ответы.
 */
internal fun tiynOf(tenge: String): Long = CabinetDecimal.of(tenge).tiyn()
