package kz.mybrain.superkassa.integrations.ncalayer

import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaLegacyRequest
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaLegacySignatureOf
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaSignRequest
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaSignatureOf
import kz.mybrain.superkassa.integrations.ncalayer.socket.NcaExchange
import java.net.URI

/**
 * NCALayer на этой машине — вебсокетом к петле.
 *
 * NCALayer слушает защищённое соединение с самоподписанным сертификатом
 * на `127.0.0.1`. Проверять его нечем и незачем: собеседник — процесс на
 * этой же машине, а не узел в сети; доверие ограничено этим соединением.
 *
 * Поэтому адрес — только петля: доверие без проверки сертификата к узлу
 * в сети превратило бы подпись владельца в подарок любому посреднику.
 *
 * @param settings адрес, сроки и имя приложения в окне подписи.
 * @param journal куда писать ход обмена.
 * @param locale язык окна подписи на момент подписи: владелец мог сменить
 *   язык приложения после запуска, и окно NCALayer говорит на нынешнем.
 * @throws IllegalArgumentException адрес не на этой машине.
 */
class DesktopNcaLayer(
    private val settings: NcaSettings = NcaSettings(),
    private val journal: NcaJournal = NcaJournal.Silent,
    private val locale: () -> String = { settings.locale }
) : NcaLayer {

    init {
        require(URI(settings.address).host in LOOPBACK) { "NCALayer address must be a loopback address" }
    }

    private val exchange = NcaExchange(settings, journal)

    /**
     * Подписывает содержимое модулем `basics`, а не вышло — прежним.
     *
     * Запасной путь идёт и при молчании нового модуля: выпуски NCALayer,
     * не знающие `basics`, не отказывают — они молчат или закрывают
     * соединение, не показав окна. Отказ владельца не повторяется.
     */
    override suspend fun sign(payload: String): String {
        val refused = try {
            return ncaSignatureOf(exchange.ask(ncaSignRequest(payload, settings.origin, locale())))
        } catch (refusal: NcaRefusal) {
            refusal
        }
        if (!refused.askPreviousModule || refused.cancelled) throw refused
        journal.record("NCALayer: basics module did not sign (${refused.reason}), asking the legacy module", null)
        return ncaLegacySignatureOf(exchange.ask(ncaLegacyRequest(payload)))
    }

    private companion object {
        /** Имена петли, под которыми NCALayer бывает указан. */
        val LOOPBACK = setOf("127.0.0.1", "localhost", "[::1]", "::1")
    }
}
