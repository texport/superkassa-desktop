package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.CounterRecord
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.history.Shift

/**
 * Состояние смены со слов узла.
 *
 * Третье состояние здесь не лишнее: пока узел не ответил, приложение
 * не знает про смену ничего, и говорить за него «закрыта» нельзя.
 */
enum class ShiftState {
    /** Узел ещё не отвечал либо ответить не смог. */
    Unknown,
    Open,
    Closed
}

/**
 * Что узел рассказал про текущую смену: документы, очередь и счётчики.
 *
 * Отдельно от сеанса, потому что это не рабочее место кассира, а снимок
 * смены: он целиком заменяется при перечитывании и целиком забывается
 * при смене кассы или кассира.
 */
class ShiftBoard {

    /**
     * Состояние смены.
     *
     * Раньше оно выводилось из того, ответил ли узел на список документов
     * текущей смены. У кассы, снятой с учёта, он отвечает KKM_BLOCKED,
     * и экран писал «Смена закрыта» над сменой 6, которую узел держал
     * открытой с 18 сентября, а «Открыть смену» получало SHIFT_ALREADY_OPEN.
     */
    var state: ShiftState by mutableStateOf(ShiftState.Unknown)
        private set

    /** Номер смены, названный узлом: он же стоит в её документах. */
    var number: Long? by mutableStateOf(null)
        private set

    /** Открыта ли смена. Закрытая смена — обычное состояние кассы утром. */
    val open: Boolean get() = state == ShiftState.Open

    val documents = mutableStateListOf<Document>()

    val queueTasks = mutableStateListOf<QueueTask>()

    /**
     * Отвечал ли узел об очереди.
     *
     * Пустая очередь и очередь, о которой узел ничего не сказал, — разные
     * вещи: первая означает, что всё доставлено, вторая не означает ничего.
     * Без этого признака экран очереди объявлял порядок, которого никто
     * не подтверждал.
     */
    var queueRead: Boolean by mutableStateOf(false)
        private set

    val counters = mutableStateListOf<CounterRecord>()

    /**
     * Наличные в денежном ящике.
     *
     * Берётся счётчик всей кассы, а не смены: кассир спрашивает, сколько
     * денег в ящике сейчас, а не сколько прошло за смену. Счётчик узел
     * отдаёт в тиынах.
     */
    val cashInDrawer: Long?
        get() = counters.firstOrNull { it.scope == GLOBAL_SCOPE && it.key == CASH_SUM }?.value

    /** Смена в том виде, в каком её назвал узел; `null` — смен ещё не было. */
    fun adoptShift(shift: Shift?) {
        state = if (shift?.status == OPEN_STATUS) ShiftState.Open else ShiftState.Closed
        number = shift?.shiftNo
        if (state != ShiftState.Open) documents.clear()
    }

    /** Узел о смене не ответил: приложение не выдумывает за него состояние. */
    fun forgetShift() {
        state = ShiftState.Unknown
        number = null
        documents.clear()
    }

    fun adoptDocuments(loaded: List<Document>) {
        documents.clear()
        documents.addAll(loaded)
    }

    fun adoptQueue(loaded: List<QueueTask>) {
        queueTasks.clear()
        queueTasks.addAll(loaded)
        queueRead = true
    }

    fun adoptCounters(loaded: List<CounterRecord>) {
        counters.clear()
        counters.addAll(loaded)
    }

    /** Смена закрыта: документов у неё нет по существу, а не по недосмотру. */
    fun closed() {
        state = ShiftState.Closed
        documents.clear()
    }

    /** Забывает всё: за машиной будет другая касса или другой кассир. */
    fun forget() {
        forgetShift()
        queueTasks.clear()
        queueRead = false
        counters.clear()
    }
}

/** Открытая смена в ответе узла. */
private const val OPEN_STATUS = "OPEN"

/** Счётчик всей кассы, а не одной смены. */
private const val GLOBAL_SCOPE = "GLOBAL"

/** Ключ счётчика наличных в ящике. */
private const val CASH_SUM = "cash.sum"
