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
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Подпись в eGov mobile через посредника.
 *
 * Два шага: [open] регистрирует процедуру и возвращает QR и ссылку
 * запуска; [await] передаёт данные и ждёт подписи. Между ними приложение
 * показывает владельцу QR и кнопку «Открыть eGov mobile», и окно стоит
 * всё время [await]. Ключ остаётся в eGov mobile, касса получает только
 * подпись.
 *
 * Данные передаются внутри [await], а не в [open]: посредник держит запрос
 * с данными, пока eGov mobile их не заберёт. Переданные сразу, они держали
 * [open] до срока, окно с QR так и не появлялось, и вход обрывался
 * словами «служба подписи не отвечает».
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
     * Регистрирует процедуру подписи; данные уйдут посреднику в [await].
     *
     * @param content что подписывается, в base64.
     * @throws EgovRefusal посредник не отвечает или отказал.
     */
    suspend fun open(content: String, document: EgovDocument): EgovProcedure {
        val asked = encoded(Registration(document.description))
        val registered = decoded<Registered>(http.post(register(), asked, REGISTER))
        registered.message?.let { throw EgovRefusal(EgovReason.Refused, it) }
        return procedureOf(registered, encoded(toSign(content, document)))
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
        hand(procedure, deadline)
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

    /**
     * Передаёт данные посреднику и ждёт, пока eGov mobile их заберёт.
     *
     * Срок — тот же, что у подписи: владелец в это время открывает
     * eGov mobile и входит в него. Не дождались до срока — срок вышел,
     * а не посредник молчит.
     */
    private suspend fun hand(procedure: EgovProcedure, deadline: TimeMark) {
        val answer = try {
            http.post(procedure.dataUrl, procedure.payload, SEND, wait = -deadline.elapsedNow())
        } catch (refusal: EgovRefusal) {
            throw expiredOr(refusal, deadline)
        }
        decoded<Signed>(answer).message?.let { throw EgovRefusal(EgovReason.Refused, it) }
    }

    /** Молчание посредника после срока — вышедший срок, а не пропавшая связь. */
    private fun expiredOr(refusal: EgovRefusal, deadline: TimeMark): EgovRefusal =
        if (refusal.reason == EgovReason.Unreachable && deadline.hasPassedNow()) {
            EgovRefusal(EgovReason.Expired)
        } else {
            refusal
        }

    /** Закрывает соединения. */
    override fun close() = http.close()

    private fun register(): String = settings.relay.trimEnd('/') + REGISTER_PATH

    /** Процедура из ответа на регистрацию; без ссылки, адресов или QR она бесполезна. */
    private fun procedureOf(registered: Registered, payload: String): EgovProcedure {
        val qr = registered.qrCode?.let { runCatching { Base64.decode(it) }.getOrNull() }
        val launch = registered.eGovMobileLaunchLink
        val sign = registered.signURL?.takeIf { registered.dataURL != null }
        if (launch == null || sign == null || qr == null) throw EgovRefusal(EgovReason.Refused, INCOMPLETE)
        return EgovProcedure(
            launch = launch,
            qr = qr,
            expiresAt = registered.expireAt?.let(Instant::fromEpochMilliseconds),
            signUrl = sign,
            dataUrl = registered.dataURL.orEmpty(),
            payload = payload
        )
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
