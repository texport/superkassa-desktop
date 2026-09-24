package kz.mybrain.superkassa.integrations.ncalayer.socket

import io.ktor.client.HttpClient
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.integrations.ncalayer.NcaJournal
import kz.mybrain.superkassa.integrations.ncalayer.NcaRefusal
import kz.mybrain.superkassa.integrations.ncalayer.NcaSettings
import kz.mybrain.superkassa.integrations.ncalayer.ncaSilent
import kz.mybrain.superkassa.integrations.ncalayer.ncaUnreachable
import kz.mybrain.superkassa.integrations.ncalayer.ncaWindowClosed
import java.io.IOException

/**
 * Один обмен с NCALayer: рукопожатие, запрос и ответ по делу.
 *
 * Отказ называет то, что случилось, а не первое похожее: «его нет»,
 * «запрос принят, ответа нет», «окно закрыли».
 */
internal class NcaExchange(private val settings: NcaSettings, private val journal: NcaJournal) {

    private val socket = NcaSocket(settings, journal)

    /** Рукопожатие, затем запрос и ответ. */
    suspend fun ask(request: JsonObject): JsonObject {
        awaitReady()
        return exchange(request)
    }

    /**
     * Убеждается, что NCALayer отвечает, прежде чем ждать подписи минутами.
     *
     * Рукопожатие делается дважды: первое часто срывается — NCALayer
     * поднимает защищённое соединение на петле, и на это ему нужно время.
     */
    private suspend fun awaitReady() {
        journal.record("NCALayer: handshake ${settings.address}", null)
        if (handshake()) return
        journal.record("NCALayer: handshake retry", null)
        if (!handshake()) throw ncaUnreachable()
    }

    private suspend fun handshake(): Boolean = try {
        withTimeout(settings.handshakeWait) { ncaClient().use { socket.knock(it) } }
        journal.record("NCALayer: handshake done", null)
        true
    } catch (timeout: TimeoutCancellationException) {
        brokenHandshake(timeout)
    } catch (failure: IOException) {
        brokenHandshake(failure)
    } catch (failure: IllegalStateException) {
        brokenHandshake(failure)
    } catch (closed: ClosedReceiveChannelException) {
        brokenHandshake(closed)
    }

    private suspend fun brokenHandshake(failure: Throwable): Boolean {
        notOwnersCancel()
        journal.record("NCALayer: handshake failed, ${failure::class.simpleName}", failure)
        return false
    }

    private suspend fun exchange(request: JsonObject): JsonObject {
        val sent = SentMark()
        val failure: Throwable = try {
            return ncaClient().use { http -> answer(http, request, sent) }
        } catch (timeout: TimeoutCancellationException) {
            timeout
        } catch (closed: ClosedReceiveChannelException) {
            closed
        } catch (broken: IOException) {
            broken
        } catch (broken: IllegalStateException) {
            broken
        }
        throw broken(failure, sent)
    }

    private suspend fun answer(http: HttpClient, request: JsonObject, sent: SentMark): JsonObject =
        withTimeout(settings.signWindow) { socket.answerTo(http, request, sent) } ?: throw ncaSilent()

    /**
     * Что значит сорванный обмен.
     *
     * Запрос не ушёл — NCALayer о нём не знает: «его нет». Ушёл, и соединение
     * закрылось сразу — окна владелец не видел: так отвечает выпуск, не
     * знающий `basics`. Закрылось позже — окно было, и закрыл его владелец.
     */
    private suspend fun broken(failure: Throwable, sent: SentMark): NcaRefusal {
        notOwnersCancel()
        return when {
            !sent.done -> ncaUnreachable(failure)
            failure is ClosedReceiveChannelException && sent.waited > settings.windowShown -> ncaWindowClosed(failure)
            else -> ncaSilent(failure)
        }
    }

    /**
     * Отмена вызывающего отменой и остаётся.
     *
     * Ловушки выше её не отличают: `withTimeout` бросает то же
     * `CancellationException`, что и отмена снаружи, а на JVM оно наследует
     * `IllegalStateException`. Выданная за молчание NCALayer, отмена вела бы
     * к повтору прежним модулем, поэтому с этой проверки начинается каждая
     * ловушка.
     */
    private suspend fun notOwnersCancel() = currentCoroutineContext().ensureActive()
}
