package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod

/**
 * Чеки и движение наличных.
 *
 * Все обращения фискальные: узел отвечает только после подтверждённой
 * публикации, поэтому приложение ждёт ответа и не считает молчание успехом.
 */
suspend fun ServerClient.sell(kkmId: String, body: ReceiptRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/receipt/sell", body, pin)

suspend fun ServerClient.buy(kkmId: String, body: ReceiptRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/receipt/buy", body, pin)

suspend fun ServerClient.sellReturn(kkmId: String, body: ReceiptRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/receipt/sell-return", body, pin)

suspend fun ServerClient.buyReturn(kkmId: String, body: ReceiptRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/receipt/buy-return", body, pin)

suspend fun ServerClient.cashIn(kkmId: String, body: CashRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/cash/in", body, pin)

suspend fun ServerClient.cashOut(kkmId: String, body: CashRequest, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/cash/out", body, pin)
