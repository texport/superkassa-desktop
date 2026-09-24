package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/**
 * Сумма, записанная в проверке так, как её пишет кассир, — в тиынах.
 *
 * «1000.05» читается и сверяется с чеком легче, чем 100_005: проверка
 * говорит о деньгах словами денег, а считает — тиынами, как касса.
 */
fun tenge(text: String): Long = checkNotNull(Tenge.parse(text)) { "not a sum: $text" }

/** Количество или доля десятичной записью: «1.5», «10». */
fun decimal(text: String): Decimal = Decimal.parse(text)
