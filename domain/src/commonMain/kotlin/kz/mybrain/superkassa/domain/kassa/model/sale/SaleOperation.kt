package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptOperationType

/**
 * Направление чека.
 *
 * Продажа кладёт деньги в кассу, покупка — выдаёт из неё. Названия
 * для кассира — в надписях области, здесь только операция кассы.
 */
enum class SaleOperation(val type: ReceiptOperationType) {
    Sell(ReceiptOperationType.SELL),
    Buy(ReceiptOperationType.BUY)
}
