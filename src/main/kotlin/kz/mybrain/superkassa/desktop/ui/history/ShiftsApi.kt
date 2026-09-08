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
