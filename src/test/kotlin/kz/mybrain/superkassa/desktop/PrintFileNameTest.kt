package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Сохранённая форма называется по документу, откуда бы её ни открыли.
 *
 * Открытие по документу задавало имя файла, а открытие по идентификатору,
 * которым оно пользуется внутри, тут же затирало его пустым — и чек
 * из документов смены сохранялся под внутренним идентификатором.
 */
class PrintFileNameTest {

    @Test
    fun `открытая по документу форма сохраняется под именем документа`() {
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        val http = HttpClient(MockEngine { respond(png, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/png")) })
        val directory = Files.createTempDirectory("print-name").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.select(Kkm(kkmId = "kkm-1"), remember = false)
        session.adoptPin("1234")
        val document = Document(id = "aae019ac-92e3", docType = "SALE", shiftNo = 1, fiscalSign = "4178697373")

        session.printDesk.preview(document)

        assertEquals("receipt-sale-shift-1-4178697373", session.printDesk.savingName)
    }
}
