package kz.mybrain.superkassa.desktop.ui.history

import io.ktor.http.HttpMethod
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ServerClient

/**
 * Прошлые смены кассы.
 *
 * Без этого обращения Z-отчёт закрытой смены нельзя было найти иначе как
 * перебором журнала за сутки — а смену, закрытую позавчера, нельзя было
 * найти вовсе.
 */
suspend fun ServerClient.shifts(kkmId: String, pin: String, offset: Int = 0): List<Shift> =
    request(HttpMethod.Get, "/kkm/$kkmId/shifts?limit=$SHIFT_PAGE&offset=$offset", pin = pin)

/**
 * Последняя смена кассы — то, что узел считает её состоянием.
 *
 * Узел отвечает на это обращение и по кассе, снятой с учёта, тогда как
 * список документов текущей смены у заблокированной кассы отказывает
 * кодом KKM_BLOCKED. Выводить открытость смены из удачи того обращения
 * значит называть закрытой смену, которую узел держит открытой.
 */
suspend fun ServerClient.lastShift(kkmId: String, pin: String): Shift? =
    request<List<Shift>>(HttpMethod.Get, "/kkm/$kkmId/shifts?limit=1", pin = pin).firstOrNull()

/** Документы одной смены, включая её открытие и закрытие. */
suspend fun ServerClient.documentsOfShift(kkmId: String, shiftId: String, pin: String): List<Document> =
    request(HttpMethod.Get, "/kkm/$kkmId/shifts/$shiftId/documents?limit=$DOCUMENT_LIMIT", pin = pin)

/**
 * Сколько смен читать за раз.
 *
 * Узел отдаёт их страницами, и журнал дочитывает следующую по требованию:
 * прежде он брал первые двести смен и молчал об остальных — касса, которой
 * год, показывала часть своей истории как всю.
 */
const val SHIFT_PAGE: Int = 200

/** Документов в смене: узел ограничивает пятьюстами. */
private const val DOCUMENT_LIMIT = 500
