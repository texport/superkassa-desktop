package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.log.bodyForLog
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.settings.OfdAuthInfo
import kz.mybrain.superkassa.desktop.ui.settings.authLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Токен кассы не попадает ни на экран, ни в журнал.
 *
 * Узел отдаёт его вместе с номером следующего запроса, и сведения
 * об узле показывали его строкой «Токен БФД: …». По токену отправляют
 * фискальные команды от имени кассы, а экран настроек показывают
 * и снимают — строка уезжала в чужой снимок вместе со всем, что рядом.
 */
class NodeFactsSecretTest {

    private val answer = """{"nextReqNum":$REQ_NUM,"token":"$TOKEN"}"""

    @Test
    fun `сведения авторизации показывают номер запроса и ничего больше`() {
        val auth = ServerClient.lenientJson.decodeFromString(OfdAuthInfo.serializer(), answer)
        val lines = authLines(LABEL, auth)

        assertEquals(listOf(LABEL to REQ_NUM.toString()), lines)
        assertFalse(
            lines.any { (name, value) -> TOKEN in name || TOKEN in value },
            "токен кассы показан на экране настроек"
        )
    }

    /** Тот же ответ в журнале: имя поля видно, значение — нет. */
    @Test
    fun `токен вырезается из журнала`() {
        val written = bodyForLog(answer).orEmpty()

        assertFalse(TOKEN in written, "токен кассы записан в журнал: $written")
        assertTrue("token" in written, "поле токена исчезло из журнала целиком")
    }

    private companion object {
        const val TOKEN = "3f5a9c1e-token-of-the-register"
        const val REQ_NUM = 4242L
        const val LABEL = "Следующий номер запроса"
    }
}
