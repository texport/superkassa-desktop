package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.integrations.egovmobile.protocol.CANCELED
import kz.mybrain.superkassa.integrations.egovmobile.protocol.Document
import kz.mybrain.superkassa.integrations.egovmobile.protocol.DocumentToSign
import kz.mybrain.superkassa.integrations.egovmobile.protocol.FileData
import kz.mybrain.superkassa.integrations.egovmobile.protocol.Registered
import kz.mybrain.superkassa.integrations.egovmobile.protocol.Registration
import kz.mybrain.superkassa.integrations.egovmobile.protocol.Signed
import kz.mybrain.superkassa.integrations.egovmobile.protocol.ToSign
import kz.mybrain.superkassa.integrations.egovmobile.protocol.decoded
import kz.mybrain.superkassa.integrations.egovmobile.protocol.encoded
import kotlin.io.encoding.Base64
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * Подпись в eGov mobile через посредника.
 *
 * Два шага, между которыми владелец подписывает: [open] регистрирует
 * процедуру и передаёт данные — и возвращает QR и ссылку запуска; [await]
 * ждёт подписи. Между ними приложение показывает владельцу QR и кнопку
 * «Открыть eGov mobile». Ключ остаётся в eGov mobile, касса получает
 * только подпись.
 *
 * @param settings адрес посредника и сроки.
 * @param journal куда писать ход обмена.
 * @param engine движок Ktor; по умолчанию — движок платформы.
 */
class EgovMobile(
    private val settings: EgovSettings = EgovSettings(),
    journal: EgovJournal = EgovJournal.Silent,
    engine: HttpClientEngine = platformEngine()
) : AutoCloseable {
    private val http = EgovHttp(settings, journal, engine)

    /**
     * Регистрирует процедуру подписи и передаёт в неё данные.
     *
     * @param content что подписывается, в base64.
     * @throws EgovRefusal посредник не отвечает или отказал.
     */
    suspend fun open(content: String, document: EgovDocument): EgovProcedure {
        val asked = encoded(Registration(document.description))
        val registered = decoded<Registered>(http.post(register(), asked, REGISTER))
        registered.message?.let { throw EgovRefusal(EgovReason.Refused, it) }
        val procedure = procedureOf(registered)
        val data = encoded(toSign(content, document))
        val sent = decoded<Signed>(http.post(registered.dataURL.orEmpty(), data, SEND))
        sent.message?.let { throw EgovRefusal(EgovReason.Refused, it) }
        return procedure
    }

    /**
     * Ждёт подписи владельца до срока [EgovSettings.signWindow].
     *
     * Посредник держит запрос, пока подписи нет; оборванный запрос
     * повторяется. Отмена вызывающего прерывает ожидание.
     *
     * @return CMS без вложенных данных, одной строкой base64.
     * @throws EgovRefusal владелец отказался, срок вышел или посредник отказал.
     */
    suspend fun await(procedure: EgovProcedure): String {
        val deadline = TimeSource.Monotonic.markNow() + settings.signWindow
        var silent: Throwable? = null
        while (deadline.hasNotPassedNow()) {
            val answer = http.poll(procedure.signUrl, -deadline.elapsedNow())
            if (answer.isSuccess) return signatureOf(decoded(answer.getOrThrow()))
            silent = answer.exceptionOrNull()
            delay(settings.retryPause)
        }
        throw silent?.let { EgovRefusal(EgovReason.Unreachable, it::class.simpleName.orEmpty(), it) }
            ?: EgovRefusal(EgovReason.Expired)
    }

    /** Закрывает соединения. */
    override fun close() = http.close()

    private fun register(): String = settings.relay.trimEnd('/') + REGISTER_PATH

    /** Процедура из ответа на регистрацию; без ссылки, адресов или QR она бесполезна. */
    private fun procedureOf(registered: Registered): EgovProcedure {
        val qr = registered.qrCode?.let { runCatching { Base64.decode(it) }.getOrNull() }
        val launch = registered.eGovMobileLaunchLink
        val sign = registered.signURL?.takeIf { registered.dataURL != null }
        if (launch == null || sign == null || qr == null) throw EgovRefusal(EgovReason.Refused, INCOMPLETE)
        return EgovProcedure(launch, qr, registered.expireAt?.let(Instant::fromEpochMilliseconds), sign)
    }

    private fun toSign(content: String, document: EgovDocument) = ToSign(
        documentsToSign = listOf(
            DocumentToSign(
                id = 1,
                nameRu = document.nameRu,
                nameKz = document.nameKk,
                nameEn = document.nameEn,
                document = Document(FileData(data = content))
            )
        )
    )

    /** Подпись из ответа посредника: отказ владельца — отдельной причиной. */
    private fun signatureOf(signed: Signed): String {
        val cms = signed.documentsToSign.firstOrNull()?.document?.file?.data?.filterNot(Char::isWhitespace)
        return cms?.takeIf { it.isNotEmpty() && signed.status != CANCELED && signed.message == null }
            ?: throw refusalOf(signed)
    }

    /** Почему в ответе нет подписи: отказ владельца, слова посредника или неполный ответ. */
    private fun refusalOf(signed: Signed): EgovRefusal = when {
        signed.status == CANCELED -> EgovRefusal(EgovReason.Cancelled)
        signed.message != null -> EgovRefusal(EgovReason.Refused, signed.message)
        else -> EgovRefusal(EgovReason.Refused, INCOMPLETE)
    }

    private companion object {
        const val REGISTER_PATH = "/api/egovQr"
        const val REGISTER = "register"
        const val SEND = "send data"
        const val INCOMPLETE = "incomplete answer"
    }
}
