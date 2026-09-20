package kz.mybrain.superkassa.desktop.ui.queue

import kz.mybrain.superkassa.desktop.server.QueueTask

/**
 * Состояние задачи очереди с точки зрения кассира.
 *
 * Узел различает пять состояний, и одно из них — «отправляется прямо
 * сейчас» — приложение раньше не знало вовсе: такая задача попадала
 * в отправленные и показывалась кассиру голым кодом IN_PROGRESS.
 */
enum class QueueState {
    /** Ждёт своей очереди. */
    Queued,

    /** Обработчик уже взял задачу. */
    Sending,

    /** Попытка не удалась; повтор возможен. */
    Failed,

    /** Доставлена в ОФД. */
    Sent,

    /**
     * Отвергнута окончательно: повтора не будет.
     *
     * Отличается от [Failed] тем, что кнопка «Повторить неудачные» её
     * не берёт: ОФД отказал по существу либо запрос не удалось собрать.
     * Разбирать такую задачу человеку.
     */
    Rejected,

    /** Состояние, которого приложение не знает. */
    Unknown;

    /**
     * Ждёт ли задача отправки.
     *
     * Неизвестное состояние считается ждущим намеренно: назвать
     * недоставленным то, что доставлено, — мелкая неточность, а назвать
     * доставленным недоставленное — потерянный документ.
     */
    val isWaiting: Boolean get() = this != Sent && this != Rejected
}

/** Состояние задачи по коду узла. */
fun queueStateOf(status: String?): QueueState = when (status) {
    "PENDING" -> QueueState.Queued
    "IN_PROGRESS" -> QueueState.Sending
    "FAILED" -> QueueState.Failed
    "SENT" -> QueueState.Sent
    "REJECTED" -> QueueState.Rejected
    else -> QueueState.Unknown
}

/** Задачи, которые ещё не дошли до ОФД. */
fun waitingTasks(tasks: List<QueueTask>): List<QueueTask> =
    tasks.filter { queueStateOf(it.status).isWaiting }

/** Задачи, которые повторяет кнопка «Повторить неудачные». */
fun failedTasks(tasks: List<QueueTask>): List<QueueTask> =
    tasks.filter { queueStateOf(it.status) == QueueState.Failed }

/** Задачи, отвергнутые окончательно: повтор их не берёт, а на экране они видны. */
fun rejectedTasks(tasks: List<QueueTask>): List<QueueTask> =
    tasks.filter { queueStateOf(it.status) == QueueState.Rejected }
