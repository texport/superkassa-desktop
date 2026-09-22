package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.server.CounterRecord
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.server.NodeVatRate
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.server.currentUser
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kz.mybrain.superkassa.desktop.ui.theme.Typeface

/**
 * Рабочее состояние кассира: выбранная касса, пин и всё, что показано на экранах.
 *
 * Пин хранится только в памяти и только на время работы приложения: он даёт
 * право на фискальные команды, и записывать его на диск нельзя.
 *
 * Предметы вынесены рядом: разговор с узлом — [NodeCalls], память рабочего
 * места — [WorkplaceSettings], снимок смены — [ShiftBoard], справочники —
 * [NodeReference]. Приём прочитанного у узла — в [SessionAdoption], подбор
 * названий касс — в [SessionKkmNames]. Сеанс подставляет их наружу под своими
 * именами, чтобы экраны спрашивали рабочее место, а не его устройство.
 */
class Session(
    val client: ServerClient = ServerClient(),
    val preferences: Preferences = Preferences()
) {
    internal val calls = NodeCalls(language = { settings.language })
    internal val settings = WorkplaceSettings(preferences)
    internal val board = ShiftBoard()
    internal val reference = NodeReference()

    var pin: String by mutableStateOf("")
        private set

    var selected: Kkm? by mutableStateOf(null)
        internal set

    /**
     * Кто вошёл: имя и роль, как их знает узел.
     *
     * Пока роль была неизвестна, рабочее место предлагало кассиру очередь
     * и настройки, а узел отвечал на них отказом.
     */
    var whoami: KkmUser? by mutableStateOf(null)
        private set

    /** Права администратора: заведение кассиров, очередь, настройки. */
    val isAdmin: Boolean get() = whoami?.role == ADMIN_ROLE

    val dictionaries: MutableMap<Dictionary, List<DictionaryEntry>> get() = reference.dictionaries
    val units: List<UnitOfMeasurement> get() = reference.units
    val vatRates: List<NodeVatRate> get() = reference.vatRates
    val referenceMissing: Boolean get() = reference.missing

    val kkms = mutableStateListOf<Kkm>()

    /** Печатная форма на экране просмотра; что там хранится — в [PrintPreview]. */
    internal val paper = PrintPreview()

    var preview: ByteArray?
        get() = paper.image
        set(value) {
            paper.image = value
        }

    var drawing: Boolean
        get() = paper.drawing
        set(value) {
            paper.drawing = value
        }

    /** Печать, просмотр и сохранение печатных форм. */
    val printDesk: PrintDesk by lazy { PrintDesk(this) }

    /** Надписи на языке кассира: одни и те же для экранов и для сеанса. */
    val texts: AppStrings get() = stringsOf(language)

    val language: Language get() = settings.language
    val appearance: Appearance get() = settings.appearance
    val look: Look get() = settings.look
    val railCollapsed: Boolean get() = settings.railCollapsed
    val placesCollapsed: Boolean get() = settings.placesCollapsed
    val rememberedWindowSize: Pair<Int, Int>? get() = settings.windowSize

    fun switchLanguage(chosen: Language) = settings.switchLanguage(chosen)
    fun switchAppearance(chosen: Appearance) = settings.switchAppearance(chosen)
    fun chooseAccent(chosen: Accent) = settings.chooseAccent(chosen)
    fun chooseTypeface(chosen: Typeface) = settings.chooseTypeface(chosen)
    fun chooseTextScale(chosen: TextScale) = settings.chooseTextScale(chosen)
    fun toggleRail() = settings.toggleRail()
    fun togglePlaces() = settings.togglePlaces()
    fun rememberWindowSize(width: Int, height: Int) = settings.rememberWindowSize(width, height)

    /** Название кассы на этом рабочем месте; откуда оно берётся — в [SessionKkmNames]. */
    fun displayName(kkm: Kkm): String = settings.nameOf(kkm)

    val nodeAvailable: Boolean get() = calls.available
    val busy: Boolean get() = calls.busy

    var lastMessage: Message?
        get() = calls.last
        set(value) {
            calls.last = value
        }

    suspend fun <T> guard(what: String, block: suspend () -> T): T? = calls.guard(what, block)

    /** Обращение, отказ по которому кассиру не показывается. */
    suspend fun <T> quietly(what: String, block: suspend () -> T): T? = calls.quiet(what, block)

    val shiftOpen: Boolean get() = board.open

    /** Состояние смены со слов узла: у него есть и «неизвестно». */
    val shiftState: ShiftState get() = board.state

    /** Номер смены, названный узлом. */
    val shiftNumber: Long? get() = board.number
    val documents: List<Document> get() = board.documents
    val queueTasks: List<QueueTask> get() = board.queueTasks
    val counters: List<CounterRecord> get() = board.counters
    val cashInDrawer: Long? get() = board.cashInDrawer

    /** Работа началась: касса выбрана и пин принят. */
    val signedIn: Boolean get() = selected != null && pin.isNotBlank()

    /** Касса, выбранная в прошлый раз. */
    val rememberedKkmId: String? get() = preferences.defaultKkmId

    fun adoptPin(newPin: String) {
        pin = newPin
    }

    /**
     * Вход кассира: пин проверяется у узла сразу.
     *
     * Без проверки кассир узнавал бы о неверном пине только на первом чеке —
     * в очереди у кассы, с покупателем перед ним.
     */
    suspend fun signIn(kkm: Kkm, enteredPin: String): Boolean {
        val user = guard(texts.login.enter) { client.currentUser(kkm.kkmId, enteredPin) } ?: return false
        whoami = user
        select(kkm)
        pin = enteredPin
        AppLog.state("вход кассира ${user.name} на кассу ${kkm.title}")
        return true
    }

    /**
     * Смена кассира: пин забывается, касса остаётся.
     *
     * Касса на рабочем месте одна и та же, а кассиры за смену меняются.
     * Заставлять нового искать её в списке — лишняя работа на ровном месте.
     */
    fun signOut() {
        whoami?.let { AppLog.state("выход кассира ${it.name}") }
        pin = ""
        whoami = null
        board.forget()
        // Пин, введённый владельцем ради печатной формы, уходит вместе
        // с кассирским: он и жил только в памяти этого сеанса.
        printDesk.drawer.forget()
        lastMessage = null
    }

    /** Уходит на выбор кассы: за этой машиной будет работать другая касса. */
    fun switchKkm() {
        signOut()
        selected = null
    }

    /**
     * Выбирает кассу и запоминает выбор до следующего запуска.
     *
     * [remember] снимается, когда кассир работает за чужой кассой разово.
     */
    fun select(kkm: Kkm, remember: Boolean = true) {
        AppLog.state("выбрана касса ${kkm.title}, состояние ${kkm.state}")
        selected = kkm
        board.forget()
        if (remember) {
            preferences.defaultKkmId = kkm.kkmId
        }
    }

    fun report(text: String) {
        lastMessage = Message.Done(text)
    }
}
