package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentPeriod
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Прочитанные страницы одного списка документов.
 *
 * Кабинет отдаёт список страницами по пятьдесят строк. Прежде экран брал
 * первую и молчал об остальных: у кассы с тысячей чеков владелец видел
 * пятьдесят и считал, что других нет.
 *
 * Состояние хранит уже показанное и то, сколько строк всего за сроком, —
 * без второго числа кнопка «показать ещё» не знает, когда ей исчезнуть.
 * Смена вида документов или срока начинается с чистого листа: смешивать
 * чеки с отчётами в одном списке нечем.
 */
class DocumentListState {

    private val loaded = mutableStateListOf<CabinetDocumentRow>()

    /** Показанные строки. */
    val rows: List<CabinetDocumentRow> get() = loaded

    /** Сколько строк у кабинета за выбранным сроком. */
    var total: Long by mutableStateOf(0)
        private set

    /** Идёт ли чтение: на это время кнопка подгрузки гаснет. */
    var loading: Boolean by mutableStateOf(false)
        private set

    /**
     * Кабинет страницы не отдал — его словами; `null` — отдал.
     *
     * Отказ держится здесь, а не берётся у сеанса: помеху сеанса каркас
     * окна забирает во всплывающую строку и тут же гасит, а список
     * обязан называть причину, пока строк на экране нет.
     */
    var trouble: String? by mutableStateOf(null)
        private set

    /** Есть ли ещё непрочитанные строки. */
    val hasMore: Boolean get() = loaded.size < total

    private var page = 0

    /**
     * Забывает прочитанное: сменился вид документов, срок или касса.
     *
     * Признак чтения снимается здесь же: чтение прошлого списка к этому
     * времени отменено вместе со своим ожиданием, и оставленный признак
     * запер бы новый список навсегда — кнопка гаснет, а первая страница
     * не читается.
     */
    fun reset() {
        loaded.clear()
        total = 0
        page = 0
        loading = false
        trouble = null
    }

    /**
     * Читает следующую страницу.
     *
     * Отказ кабинета оставляет прочитанное на месте и не двигает счётчик
     * страниц: повторное нажатие возьмёт ту же страницу, а не пропустит её.
     */
    suspend fun loadNext(
        cabinet: CabinetSession,
        token: String,
        registerId: String,
        kind: DocumentKind,
        period: DocumentPeriod,
        texts: CabinetTexts
    ) {
        if (loading) return
        loading = true
        val slice = try {
            cabinet.guard { loadDocuments(cabinet, token, registerId, kind, page, period, texts) }
        } finally {
            loading = false
        }
        if (slice == null) {
            trouble = cabinet.problem?.let { cabinetMessage(it, texts).words() } ?: texts.unreachable
            return
        }
        trouble = null
        loaded.addAll(slice.rows)
        total = slice.total
        page += 1
    }

    /**
     * Что стоит за строкой журнала.
     *
     * Журнал показывает строки, не зная, откуда они пришли, и обратно
     * к записи кабинета ведёт ключ строки: у чека, отчёта и движения
     * денег это идентификатор операции, у смены — её номер.
     */
    fun targetOf(key: String): RowTarget? = loaded.firstOrNull { it.entry.key == key }?.target
}
