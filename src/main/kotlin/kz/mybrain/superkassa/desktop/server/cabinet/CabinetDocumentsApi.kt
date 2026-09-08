package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Фискальные документы кассы глазами ОФД.
 *
 * Продолжение [CabinetApi]: чеки, смены, отчёты и движение денег —
 * то, что принял сервер приёма данных, а не то, что лежит в узле.
 */

// --- Документы ---

suspend fun CabinetClient.documentsOverview(token: String, id: String): DocumentsOverview =
    request(HttpMethod.Get, "/api/cash-registers/$id/documents", token = token)

suspend fun CabinetClient.receipts(token: String, id: String, search: ReceiptSearch): CabinetPage<CabinetReceipt> =
    request(HttpMethod.Post, "/api/cash-registers/$id/receipts/search", search, token)

suspend fun CabinetClient.shifts(token: String, id: String, page: Int = 0): CabinetPage<CabinetShift> =
    request(HttpMethod.Get, "/api/cash-registers/$id/shifts?page=$page&size=$PAGE_SIZE", token = token)

suspend fun CabinetClient.reports(token: String, id: String, page: Int = 0): CabinetPage<CabinetReport> =
    request(HttpMethod.Get, "/api/cash-registers/$id/reports?page=$page&size=$PAGE_SIZE", token = token)

suspend fun CabinetClient.cashMovements(
    token: String,
    id: String,
    page: Int = 0
): CabinetPage<CabinetCashMovement> =
    request(HttpMethod.Get, "/api/cash-registers/$id/cash-movements?page=$page&size=$PAGE_SIZE", token = token)

/** Чек целиком: состав, оплаты, налоги и отметка КГД. */
suspend fun CabinetClient.receipt(token: String, id: String, transactionId: String): CabinetReceiptDetails =
    request(HttpMethod.Get, "/api/cash-registers/$id/receipts/$transactionId", token = token)

/** Отчёт целиком. */
suspend fun CabinetClient.report(token: String, id: String, transactionId: String): CabinetReportDetails =
    request(HttpMethod.Get, "/api/cash-registers/$id/reports/$transactionId", token = token)

/** Внесение или изъятие целиком. */
suspend fun CabinetClient.cashMovement(
    token: String,
    id: String,
    transactionId: String
): CabinetCashMovementDetails =
    request(HttpMethod.Get, "/api/cash-registers/$id/cash-movements/$transactionId", token = token)
