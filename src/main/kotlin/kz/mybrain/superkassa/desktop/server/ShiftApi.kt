package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod

/**
 * Смена, чеки, деньги и отчёты.
 *
 * Все эти обращения фискальные: узел отвечает только после подтверждённой
 * публикации, поэтому приложение ждёт ответа и не считает молчание успехом.
 */
suspend fun ServerClient.openShift(kkmId: String, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/shift/open", pin = pin)

suspend fun ServerClient.closeShift(kkmId: String, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/shift/close", pin = pin)

suspend fun ServerClient.xReport(kkmId: String, pin: String): FiscalResult =
    request(HttpMethod.Post, "/kkm/$kkmId/report", pin = pin)
