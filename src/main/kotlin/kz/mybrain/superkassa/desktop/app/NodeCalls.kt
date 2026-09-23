package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.server.ServerRefusal
import kz.mybrain.superkassa.desktop.ui.strings.Language

/**
 * Обращения к узлу: занятость, отказы и недоступность.
 *
 * Отдельно от сеанса, потому что предмет здесь один — разговор с узлом,
 * а не рабочее место кассира. Сеанс подставляет это наружу под своими
 * именами: экраны спрашивают `session.busy`, а не про внутреннее
 * устройство разговора.
 */
class NodeCalls(
    /**
     * На каком языке говорить отказом узла.
     *
     * Узел отвечает на трёх языках сразу, и выбор языка — это выбор
     * кассира, сделанный в шапке. Спрашивается при каждом отказе:
     * язык меняют посреди работы.
     */
    private val language: () -> Language = { Language.Ru }
) {

    /** Отвечает ли узел. Недоступность — повод звать обслуживание. */
    var available: Boolean by mutableStateOf(false)
        private set

    /** Последнее, что нужно сказать кассиру. */
    var last: Message? by mutableStateOf(null)

    /** Сколько обращений сейчас в работе. */
    private var pending: Int by mutableStateOf(0)

    /**
     * Идёт ли сейчас обращение к узлу.
     *
     * Кассир жмёт «Пробить чек» и ждёт ответа ОФД секунду-другую. Без
     * признака занятости экран в этот момент неотличим от непринятого
     * нажатия, и чек пробивают второй раз.
     */
    val busy: Boolean get() = pending > 0

    /** Узел ответил по существу — связь есть. */
    fun answered() {
        available = true
    }

    /**
     * Выполняет обращение, о котором кассиру знать незачем.
     *
     * Отличается от [guard] молчанием: строка сообщения принадлежит
     * тому, что кассир нажал. Перенос названий касс из кабинета он
     * не затевал, и отказ по нему на экране объяснял бы действие,
     * которого не было. Отказ уходит в журнал.
     *
     * @return `null`, если узел отказал или не ответил.
     */
    suspend fun <T> quiet(what: String, block: suspend () -> T): T? {
        pending++
        return try {
            block()
        } catch (refusal: ServerRefusal) {
            AppLog.warn(LogSource.Node, "$what: отказ ${refusal.code}")
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AppLog.warn(LogSource.Node, "$what: узел не ответил — ${failure::class.simpleName}")
            null
        } finally {
            pending--
        }
    }

    /**
     * Выполняет обращение к узлу, отделяя отказ по существу от недоступности.
     *
     * Отказ узла показывается кассиру его же словами; недоступность узла —
     * отдельным сообщением, потому что действия кассира разные: в первом
     * случае исправить чек, во втором позвать обслуживание.
     */
    suspend fun <T> guard(what: String, block: suspend () -> T): T? {
        // Сообщение снимается до обращения: строка обязана относиться
        // к последнему действию. Иначе отказ прошлого экрана висит над
        // удачным чеком и сдвигает разметку под кассиром.
        last = null
        // Обращения считаются, а не помечаются флагом: пока идёт печать,
        // экран успевает запросить справочник, и первый же завершившийся
        // запрос гасил бы индикатор посреди работы второго.
        pending++
        return try {
            block()
        } catch (refusal: ServerRefusal) {
            // Отказ по существу — это ответ: узел на связи и разговаривает.
            // Прежде связь считалась только по удачным обращениям, и у узла,
            // отвечавшего отказом, шапка писала «Узел недоступен» — кассир
            // звал обслуживание к узлу, который работает.
            available = true
            val words = refusal.words.of(language())
            // Ответ узла идёт в журнал, а не кассиру: он бывает
            // не разбираемым вовсе, и тогда кассир читает слова
            // приложения, а обслуживание — то, что пришло с той стороны.
            val said = refusal.answer?.let { " (ответ узла: $it)" }.orEmpty()
            AppLog.warn(LogSource.Node, "$what: отказ ${refusal.code} — $words$said")
            last = Message.Refusal(words, refusal.code)
            null
        } catch (cancelled: CancellationException) {
            // Экран закрыли, не дождавшись ответа. Это не сбой узла:
            // показывать «узел недоступен» на уход с экрана нельзя.
            throw cancelled
        } catch (failure: Exception) {
            AppLog.record(LogSource.Node, LogLevel.Failure, "$what: узел не ответил — ${failure::class.simpleName}")
            // Не дождались ответа — не то же, что узел не отвечает вовсе.
            // Узел обращение принял и мог довести его до конца: чек
            // фискализируется и уходит в БФД дольше, чем касса ждёт
            // на медленной связи. «Узел недоступен» здесь отправляло
            // кассира пробивать чек второй раз.
            if (timedOut(failure)) {
                last = Message.NoAnswer(what)
                return null
            }
            available = false
            // Кассиру — что узел не ответил и на чём именно; имя исключения
            // остаётся в журнале. Прежде на экране стояло «ConnectException»,
            // а кассиру от этого слова нет никакой пользы.
            last = Message.NodeUnavailable(what)
            null
        } finally {
            pending--
        }
    }
}

/**
 * Не дождались ли ответа.
 *
 * Обрыв соединения и отказ в соединении означают недоступный узел,
 * а истекшее ожидание — что узел обращение принял и мог его выполнить.
 * Различие видно кассиру: в первом случае касса ничего не пробила,
 * во втором чек мог уже стать фискальным.
 */
private fun timedOut(failure: Throwable): Boolean =
    failure is HttpRequestTimeoutException || failure is SocketTimeoutException
