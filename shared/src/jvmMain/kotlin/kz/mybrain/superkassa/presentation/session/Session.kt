package kz.mybrain.superkassa.presentation.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.node.CoreBridge
import kz.mybrain.superkassa.data.node.CounterRecord
import kz.mybrain.superkassa.data.node.Dictionary
import kz.mybrain.superkassa.data.node.DictionaryEntry
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.Kkm
import kz.mybrain.superkassa.data.node.KkmUser
import kz.mybrain.superkassa.data.node.NodeVatRate
import kz.mybrain.superkassa.data.node.QueueTask
import kz.mybrain.superkassa.data.node.ServerClient
import kz.mybrain.superkassa.data.node.UnitOfMeasurement
import kz.mybrain.superkassa.data.node.currentUser
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.domain.signin.SignInState
import kz.mybrain.superkassa.presentation.messages.Message
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.print.PreviewTrouble
import kz.mybrain.superkassa.presentation.print.PrintDesk
import kz.mybrain.superkassa.presentation.print.PrintPreview
import kz.mybrain.superkassa.presentation.sale.DomainKind
import kz.mybrain.superkassa.presentation.strings.AppStrings
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.stringsOf

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
    val preferences: Preferences = Preferences(),
    /** Вход, сделанный в кассе процесса: сеанс его повторяет для разделов на узле. */
    val signIn: SignIn = SignIn(),
    /** Строка сообщений окна: одна на сеанс и на модели экранов. */
    val notices: Notices = Notices()
) {
    internal val calls = NodeCalls(notices, language = { settings.language })
    internal val settings = WorkplaceSettings(preferences)
    internal val board = ShiftBoard()
    internal val reference = NodeReference()

    var pin: String by mutableStateOf("")
        internal set

    var selected: Kkm? by mutableStateOf(null)
        internal set

    /**
     * Кто вошёл: имя и роль, как их знает узел.
     *
     * Пока роль была неизвестна, рабочее место предлагало кассиру очередь
     * и настройки, а узел отвечал на них отказом.
     */
    var whoami: KkmUser? by mutableStateOf(null)
        internal set

    /** Права администратора: заведение кассиров, очередь, настройки. */
    val isAdmin: Boolean get() = whoami?.role == ADMIN_ROLE

    val dictionaries: MutableMap<Dictionary, List<DictionaryEntry>> get() = reference.dictionaries
    val units: List<UnitOfMeasurement> get() = reference.units
    val vatRates: List<NodeVatRate> get() = reference.vatRates
    val referenceMissing: Boolean get() = reference.missing

    val kkms = mutableStateListOf<Kkm>()

    /** Читался ли список касс: пустой список и непрочитанный — разные вещи. */
    var kkmsRead: Boolean by mutableStateOf(false)
        internal set

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

    /** Узел не нарисовал открытую форму; `null` — беды нет. */
    var previewTrouble: PreviewTrouble?
        get() = paper.trouble
        set(value) {
            paper.trouble = value
        }

    /** Печать, просмотр и сохранение печатных форм. */
    val printDesk: PrintDesk by lazy { PrintDesk(this) }

    /** Установленная версия и найденные выпуски; спрашивает GitHub, а не узел. */
    val updates: Updates by lazy { Updates(preferences.updates) }

    /** Надписи на языке кассира: одни и те же для экранов и для сеанса. */
    val texts: AppStrings get() = stringsOf(language)

    val language: Language get() = settings.language

    fun switchLanguage(chosen: Language) = settings.switchLanguage(chosen)

    /**
     * Отрасль, в которой работает выбранная касса.
     *
     * Спрашивают её и экран продажи, и сборка чека: от неё зависит, какие
     * реквизиты кассир заполняет и какой подблок уходит в БФД. Отрасль
     * держится за кассой: на одной машине их бывает несколько, и отрасли
     * у них разные.
     */
    val domain: DomainKind get() = settings.domainOf(selected?.kkmId)

    fun chooseDomain(kkmId: String, chosen: DomainKind) = settings.chooseDomain(kkmId, chosen)

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
    /** Когда узел открыл смену; по суткам открытой смены он блокирует кассу. */
    val shiftOpenedAt: Long? get() = board.openedAt

    val documents: List<Document> get() = board.documents

    /** Отвечал ли узел о документах смены: пустой список и молчание — разные вещи. */
    val documentsRead: Boolean get() = board.documentsRead
    val queueTasks: List<QueueTask> get() = board.queueTasks

    /** Отвечал ли узел об очереди: пустая очередь и молчание — разные вещи. */
    val queueRead: Boolean get() = board.queueRead
    val counters: List<CounterRecord> get() = board.counters
    val cashInDrawer: Long? get() = board.cashInDrawer

    /** Работа началась: касса выбрана и пин принят. */
    val signedIn: Boolean get() = selected != null && pin.isNotBlank()

    /** Касса, выбранная в прошлый раз. */
    val rememberedKkmId: String? get() = preferences.defaultKkmId

    fun adoptPin(newPin: String) {
        pin = newPin
        signIn.changePin(newPin)
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
        signIn.signOut()
    }

    /** Уходит на выбор кассы: за этой машиной будет работать другая касса. */
    fun switchKkm() {
        signOut()
        selected = null
        signIn.switchKkm()
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

    /**
     * Повторяет в сеансе вход, сделанный в кассе процесса.
     *
     * Временный мост: вход и главный экран работают с ядром, а шапка и разделы,
     * ещё не переведённые с узла, читают кассу, пин и роль из сеанса. Сеанс
     * здесь ничего не решает сам — только переписывает то, что назвал
     * держатель входа. Уходит вместе с сеансом.
     */
    internal fun adoptSignIn(state: SignInState) {
        val kkm = state.kkm?.let(CoreBridge::kkm)
        if (selected?.kkmId != kkm?.kkmId) board.forget()
        selected = kkm
        pin = state.pin
        whoami = state.cashier?.let(CoreBridge::user)
    }

    /**
     * Следит за держателем входа, пока открыто окно.
     *
     * Первое состояние пропускается: сеанс, вошедший по-старому, через узел,
     * пустым держателем не затирается. Перенос идёт без очереди — в том же
     * потоке, где держатель сменился, — чтобы окно не успело нарисовать
     * рабочее место без кассы.
     */
    suspend fun followSignIn() = withContext(Dispatchers.Unconfined) {
        signIn.state.drop(1).collect { adoptSignIn(it) }
    }

    /** Печатная форма документа кассы процесса — пока печать живёт на узле. */
    fun previewDocument(document: FiscalDocumentResponse) = printDesk.preview(CoreBridge.document(document))

    /** Печать документа кассы процесса на принтер рабочего места — пока печать живёт на узле. */
    fun printDocument(document: FiscalDocumentResponse) = printDesk.print(CoreBridge.document(document))
}
