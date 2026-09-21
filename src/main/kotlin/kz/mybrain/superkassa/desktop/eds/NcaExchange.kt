package kz.mybrain.superkassa.desktop.eds

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Один обмен с NCALayer: соединение, запрос и ответ по делу.
 *
 * Отказ здесь называет то, что случилось, а не первое похожее: «его нет»,
 * «запрос принят, ответа нет», «окно закрыли». Прежде всё это приходило
 * одним отказом связи, и владелец читал «Запустите NCALayer» про
 * работающий. Ход обмена пишется в журнал — см. [ncaJournal].
 *
 * @param signWindow сколько ждать подписи после отправки запроса.
 */
internal class NcaExchange(private val address: String, private val signWindow: Duration) {

    /** Рукопожатие, запрос и ответ по делу. */
    suspend fun ask(request: JsonObject): JsonObject {
        awaitReady()
        return exchange(request)
    }

    /**
     * Убеждается, что NCALayer отвечает, прежде чем ждать подписи.
     *
     * Ожидание подписи длинное намеренно — столько владелец выбирает
     * сертификат и вводит пароль. Но если NCALayer не отвечает вовсе,
     * ждать эти минуты незачем: короткое рукопожатие отделяет «его нет»
     * от «человек ещё думает».
     *
     * Рукопожатие делается дважды: первое к NCALayer часто срывается —
     * он поднимает защищённое соединение на петле, и на это ему нужно
     * время. Владелец видел отказ и повторял вручную.
     */
    private suspend fun awaitReady() {
        ncaJournal("рукопожатие $address")
        if (handshake()) return
        ncaJournal("повтор рукопожатия")
        if (handshake()) return
        throw ncaUnreachable()
    }

    /** Отвечает ли NCALayer: подключились и разошлись. */
    private suspend fun handshake(): Boolean = try {
        withTimeout(HANDSHAKE) { ncaClient().use { it.knock() } }
        ncaJournal("рукопожатие прошло")
        true
    } catch (timeout: TimeoutCancellationException) {
        notOwnersCancel()
        ncaJournalBroken("рукопожатие", timeout)
        false
    } catch (failure: IOException) {
        notOwnersCancel()
        ncaJournalBroken("рукопожатие", failure)
        false
    } catch (failure: IllegalStateException) {
        notOwnersCancel()
        ncaJournalBroken("рукопожатие", failure)
        false
    } catch (closed: ClosedReceiveChannelException) {
        notOwnersCancel()
        ncaJournalBroken("рукопожатие", closed)
        false
    }

    /** Запрос и ответ на него; отмена владельца проходит насквозь — см. [broken]. */
    private suspend fun exchange(request: JsonObject): JsonObject {
        val sent = Sent()
        return try {
            ncaClient().use { http ->
                withTimeout(signWindow) { http.answerTo(request, sent) } ?: throw ncaSilent()
            }
        } catch (timeout: TimeoutCancellationException) {
            throw broken(timeout, sent)
        } catch (closed: ClosedReceiveChannelException) {
            throw broken(closed, sent)
        } catch (failure: IOException) {
            throw broken(failure, sent)
        } catch (failure: IllegalStateException) {
            throw broken(failure, sent)
        }
    }

    /**
     * Обмен по поднятому соединению: запрос и первый ответ по делу.
     *
     * Соединение обрывается сразу, как ответ получен, а не закрывается
     * вежливо. Вежливое закрытие ждёт закрывающего кадра от собеседника,
     * а NCALayer его не присылает: подпись уже была в руках, а приложение
     * стояло до истечения срока ожидания и потом объявляло отказ —
     * владелец подписал, и подпись выбрасывалась.
     */
    private suspend fun HttpClient.answerTo(request: JsonObject, sent: Sent): JsonObject? {
        val session = webSocketSession(address)
        return try {
            session.send(Frame.Text(request.toString()))
            sent.mark = TimeSource.Monotonic.markNow()
            ncaJournal("запрос отправлен: ${ncaAddressee(request)}")
            session.answerFrame().also {
                ncaJournal(if (it == null) "ответа нет" else "ответ получен")
            }
        } finally {
            session.cancel()
        }
    }

    /**
     * Стук в дверь: соединение поднято и тут же оборвано.
     *
     * Здороваться незачем — нужен только ответ на вопрос, отвечает ли
     * NCALayer вообще; а прощание зависит от собеседника и висло бы.
     */
    private suspend fun HttpClient.knock() {
        webSocketSession(address).cancel()
    }

    /**
     * Ответ по делу из очереди кадров.
     *
     * NCALayer здоровается первым: сразу после подключения он присылает
     * кадр со своей версией. Принимать его за ответ нельзя — подпись
     * приходит следующим кадром, и приложение выбрасывало её, открывая
     * окно подписи во второй раз.
     */
    private suspend fun DefaultClientWebSocketSession.answerFrame(): JsonObject? {
        while (true) {
            val text = (incoming.receive() as? Frame.Text)?.readText() ?: continue
            val frame = parsed(text)
            ncaJournalFrame(frame, text.length)
            if (frame != null && !frame.isGreeting()) return frame
        }
    }

    /**
     * Что значит сорванный обмен.
     *
     * Первым делом — не отмена ли это владельца: см. [notOwnersCancel].
     *
     * Запрос не ушёл — NCALayer о нём не знает, и это «его нет». Ушёл,
     * и соединение закрылось сразу — окна подписи владелец не видел:
     * так отвечает выпуск, не знающий модуль `basics`. Закрылось позже —
     * окно было, и закрыл его владелец: повторять нечего.
     */
    private suspend fun broken(failure: Throwable, sent: Sent): EdsRefusal {
        notOwnersCancel()
        return when {
            !sent.done -> ncaUnreachable(failure)
            failure is ClosedReceiveChannelException && sent.waited > WINDOW_SHOWN -> ncaWindowClosed(failure)
            else -> ncaSilent(failure)
        }
    }

    /**
     * Разбирает кадр; неразборный кадр ответом не считается.
     *
     * Единственный `runCatching` в обмене: разбор строки ничего не ждёт,
     * и отмене здесь взяться негде — см. [notOwnersCancel].
     */
    private fun parsed(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()

    /**
     * Отмена владельца отменой и остаётся.
     *
     * Ловушки выше её не отличают: `withTimeout` бросает то же
     * `CancellationException`, что и отмена снаружи, а на JVM оно ещё
     * и наследует `IllegalStateException` — то есть попадает в ловушку
     * сорванного соединения. Выданная за молчание NCALayer, отмена вела
     * к повтору прежним модулем и отказу на экране, поэтому с этой
     * проверки начинается каждая ловушка.
     */
    private suspend fun notOwnersCancel() = currentCoroutineContext().ensureActive()

    /** Ушёл ли запрос и сколько прошло с тех пор. */
    private class Sent {
        var mark: TimeMark? = null
        val done: Boolean get() = mark != null
        val waited: Duration get() = mark?.elapsedNow() ?: Duration.ZERO
    }

    private companion object {
        /**
         * Сколько ждать рукопожатия: столько занимает поднять защищённое
         * соединение на петле. Человек в этом не участвует, и ждать
         * дольше нечего.
         */
        val HANDSHAKE: Duration = 5.seconds

        /**
         * С какой задержкой закрытое соединение означает закрытое окно.
         *
         * Быстрее человек его не закроет — он его ещё не увидел: значит,
         * соединение закрыл сам NCALayer, не поняв запроса.
         */
        val WINDOW_SHOWN: Duration = 3.seconds

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
