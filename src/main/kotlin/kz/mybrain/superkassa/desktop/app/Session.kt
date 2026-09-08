package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.desktop.server.CounterRecord
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.ServerRefusal
import kz.mybrain.superkassa.desktop.server.counters
import kz.mybrain.superkassa.desktop.server.currentUser
import kz.mybrain.superkassa.desktop.server.dictionary
import kz.mybrain.superkassa.desktop.server.listKkms
import kz.mybrain.superkassa.desktop.server.queue
import kz.mybrain.superkassa.desktop.server.shiftDocuments
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.documentFallback
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Appearance

/**
 * Рабочее состояние кассира: выбранная касса, пин и всё, что показано на экранах.
 *
 * Пин хранится только в памяти и только на время работы приложения: он даёт
 * право на фискальные команды, и записывать его на диск нельзя.
 */
class Session(
    val client: ServerClient = ServerClient(),
    val preferences: Preferences = Preferences()
) {

    var pin: String by mutableStateOf("")
        private set

    var selected: Kkm? by mutableStateOf(null)
        private set

    var nodeAvailable: Boolean by mutableStateOf(false)
        private set

    /**
     * Открыта ли смена на выбранной кассе.
     *
     * Закрытая смена — обычное состояние кассы утром, а не сбой. Пока узел
     * отвечал на список документов отказом SHIFT_NOT_OPEN, кассир каждое
     * утро видел красную полосу и искал, что сломалось.
     */
    var shiftOpen: Boolean by mutableStateOf(false)
        private set

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

    var lastMessage: Message? by mutableStateOf(null)

    /**
     * Язык интерфейса.
     *
     * Касса работает в Казахстане, поэтому по умолчанию — государственный.
     * Выбор запоминается: кассир меняет язык один раз, а не каждое утро.
     */
    var appearance: Appearance by mutableStateOf(Appearance.byCode(preferences.appearance))
        private set

    var language: Language by mutableStateOf(Language.byCode(preferences.language))
        private set

    /**
     * Надписи текущего языка.
     *
     * Сессия сообщает кассиру, что именно не получилось: «документы смены»,
     * «очередь». Эти подписи обязаны звучать на том же языке, что и экран.
     */
    /** Надписи на языке кассира: одни и те же для экранов и для сеанса. */
    val texts: AppStrings get() = stringsOf(language)

    /** Справочники узла: виды оплаты, состояния доставки и прочее. */
    val dictionaries = mutableStateMapOf<Dictionary, List<DictionaryEntry>>()

    /** Свои названия касс: ключ — касса, значение — как её зовут здесь. */
    private val localNames = mutableStateMapOf<String, String>()

    /**
     * Печатная форма, открытая кассиром.
     *
     * Живёт в сеансе, а не на экране: печать идёт секунду-другую, кассир
     * успевает уйти в другой раздел, и запущенное экраном обращение
     * обрывалось вместе с ним — форма не открывалась вовсе.
     */
    var preview: ByteArray? by mutableStateOf(null)

    /** Свёрнут ли рельс разделов: подписи спрятаны, значки остались. */
    var railCollapsed: Boolean by mutableStateOf(preferences.railCollapsed)
        private set

    /** Сворачивает и разворачивает рельс; выбор запоминается. */
    fun toggleRail() {
        railCollapsed = !railCollapsed
        preferences.railCollapsed = railCollapsed
    }

    /** Печать, просмотр и сохранение печатных форм. */
    val printDesk: PrintDesk by lazy { PrintDesk(this) }

    /** Сколько обращений к узлу сейчас в работе. */
    private var pending: Int by mutableStateOf(0)

    val kkms = mutableStateListOf<Kkm>()
    val documents = mutableStateListOf<Document>()
    val queueTasks = mutableStateListOf<QueueTask>()
    val counters = mutableStateListOf<CounterRecord>()

    /** Работа началась: касса выбрана и пин принят. */
    val signedIn: Boolean get() = selected != null && pin.isNotBlank()

    /** Касса, выбранная в прошлый раз. */
    val rememberedKkmId: String? get() = preferences.defaultKkmId

    /**
     * Вход кассира: пин проверяется у узла сразу.
     *
     * Без проверки кассир узнавал бы о неверном пине только на первом чеке —
     * в очереди у кассы, с покупателем перед ним.
     */
    fun adoptPin(newPin: String) {
        pin = newPin
    }

    suspend fun signIn(kkm: Kkm, enteredPin: String): Boolean {
        val user = guard(texts.login.enter) {
            client.currentUser(kkm.kkmId, enteredPin)
        } ?: return false
        whoami = user
        select(kkm)
        pin = enteredPin
        return true
    }

    fun switchLanguage(chosen: Language) {
        language = chosen
        preferences.language = chosen.code
    }

    /** Размер окна, каким кассир оставил его в прошлый раз. */
    val rememberedWindowSize: Pair<Int, Int>? get() = preferences.windowSize

    /** Запоминает размер окна: на кассовом столе монитор не меняется. */
    fun rememberWindowSize(width: Int, height: Int) {
        preferences.windowSize = width to height
    }

    /** Выбор светлой или тёмной кассы держится этого рабочего места. */
    fun switchAppearance(chosen: Appearance) {
        appearance = chosen
        preferences.appearance = chosen.code
    }

    /**
     * Читает справочники узла.
     *
     * Названия видов оплаты, состояний и документов приходят с узла сразу
     * на трёх языках: свой перевод в приложении рано или поздно разошёлся бы
     * с тем, что напечатано на чеке.
     */
    suspend fun loadDictionaries() {
        Dictionary.entries.forEach { dictionary ->
            guard<Unit>(texts.settings.title) {
                dictionaries[dictionary] = client.dictionary(dictionary)
            }
        }
    }

    /**
     * Дочитывает справочники, которые в прошлый раз не пришли.
     *
     * Читаются они один раз при входе, и узел в этот миг мог быть
     * недоступен: тогда список видов оплаты или перечень ОФД оставался
     * пустым до конца смены, а форма заведения кассы — незаполнимой.
     * Перечитывается только пустое: пришедшее менять незачем.
     */
    suspend fun loadMissingDictionaries() {
        val missing = Dictionary.entries.filter { dictionaries[it].isNullOrEmpty() }
        if (missing.isEmpty()) return
        missing.forEach { dictionary ->
            guard<Unit>(texts.settings.title) {
                dictionaries[dictionary] = client.dictionary(dictionary)
            }
        }
    }

    /**
     * Название кассы на этом рабочем месте: своё, если задано.
     *
     * Названия держатся состоянием, а не читаются из файла при каждом
     * обращении: файл Compose не наблюдает, и переименованная касса
     * оставалась на экране под прежним названием до перезапуска.
     */
    fun displayName(kkm: Kkm): String = localNames[kkm.kkmId] ?: kkm.title

    fun rename(kkm: Kkm, name: String?) {
        val chosen = name?.takeIf { it.isNotBlank() }
        preferences.rename(kkm.kkmId, chosen)
        if (chosen == null) localNames.remove(kkm.kkmId) else localNames[kkm.kkmId] = chosen
    }

    /**
     * Название значения справочника на языке кассира.
     *
     * Если узел такого кода не знает — а в журнале остались записи
     * прежних версий, — берётся своё название. Голый код на экране кассы
     * недопустим.
     */
    fun titleOf(dictionary: Dictionary, code: String?): String {
        if (code == null) return "—"
        val fromNode = dictionaries[dictionary]?.firstOrNull { it.code == code }?.title(language.code)
        if (fromNode != null && fromNode != code) return fromNode
        val own = if (dictionary == Dictionary.DocumentTypes) {
            texts.enums.documentFallback(code)
        } else {
            null
        }
        return own ?: code
    }

    /**
     * Смена кассира: пин забывается, касса остаётся.
     *
     * Касса на рабочем месте одна и та же, а кассиры за смену меняются.
     * Заставлять нового искать её в списке — лишняя работа на ровном месте.
     */
    fun signOut() {
        pin = ""
        whoami = null
        documents.clear()
        queueTasks.clear()
        counters.clear()
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
        selected = kkm
        documents.clear()
        queueTasks.clear()
        counters.clear()
        if (remember) {
            preferences.defaultKkmId = kkm.kkmId
        }
    }

    /**
     * Перечитывает список касс. Отсутствие узла — не ошибка кассира.
     *
     * Итог только что выполненного действия переживает перечитывание:
     * перечитывание — это не действие кассира, и стирать им ответ на то,
     * что кассир нажал, нельзя.
     */
    suspend fun refreshKkms(): Unit? {
        val previous = lastMessage
        loadMissingDictionaries()
        val result = guard<Unit>(texts.login.reload) {
            val loaded = client.listKkms()
            kkms.clear()
            kkms.addAll(loaded)
            // Свои названия перечитываются вместе со списком: до первого
            // обращения к настройкам их в состоянии нет, а список и вход
            // должны показывать кассу так, как её назвали здесь.
            loaded.forEach { kkm ->
                preferences.localName(kkm.kkmId)?.let { localNames[kkm.kkmId] = it }
            }
            nodeAvailable = true
            selected?.let { current ->
                selected = loaded.firstOrNull { it.kkmId == current.kkmId } ?: current
            }
        }
        if (lastMessage == null) lastMessage = previous
        return result
    }

    /**
     * Перечитывает всё, что относится к выбранной кассе.
     *
     * Обновление идёт тремя обращениями, и каждое чистит сообщение за собой.
     * Первый отказ запоминается и возвращается в строку в конце: иначе
     * удачное третье обращение стирало бы отказ первого, и кассир не узнал
     * бы, что документы не перечитались.
     */
    suspend fun refreshSelected() {
        val kkm = selected ?: return
        // Итог только что выполненного действия переживает перечитывание:
        // иначе «чек пробит» стирается обновлением, которое само это
        // действие и вызвало, и кассир остаётся без подтверждения.
        // Отказ переживает его наравне с удачей: «очередь не отправлена»
        // кассиру нужнее, чем молчание.
        val done = lastMessage
        val problems = mutableListOf<Message>()
        guard<Unit>(texts.dashboard.shiftDocuments) {
            val loaded = client.shiftDocuments(kkm.kkmId, pin)
            documents.clear()
            documents.addAll(loaded)
            shiftOpen = true
        }
        val documentsProblem = lastMessage
        if (documentsProblem is Message.Refusal && documentsProblem.code == SHIFT_NOT_OPEN) {
            // Смена закрыта — это состояние, а не отказ.
            shiftOpen = false
            documents.clear()
            lastMessage = null
        } else {
            documentsProblem?.let(problems::add)
        }
        // Очередь узел показывает только администратору: кассиру её не
        // спрашиваем, иначе вход встречал бы его отказом на пустом месте.
        if (isAdmin) {
            guard<Unit>(texts.queue.title) {
                val loaded = client.queue(kkm.kkmId, pin)
                queueTasks.clear()
                queueTasks.addAll(loaded)
            }
            lastMessage?.let(problems::add)
        } else {
            queueTasks.clear()
        }
        guard<Unit>(texts.dashboard.cashInDrawer) {
            val loaded = client.counters(kkm.kkmId, pin)
            counters.clear()
            counters.addAll(loaded)
        }
        lastMessage?.let(problems::add)
        // Ответ на нажатую кнопку сильнее беды перечитывания: кассир
        // спрашивал «что сделала кнопка», и подменять этот ответ отказом
        // фонового чтения значит не ответить вовсе.
        lastMessage = done ?: problems.firstOrNull()
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
        lastMessage = null
        // Обращения считаются, а не помечаются флагом: пока идёт печать,
        // экран успевает запросить справочник, и первый же завершившийся
        // запрос гасил бы индикатор посреди работы второго.
        pending++
        return try {
            block()
        } catch (refusal: ServerRefusal) {
            lastMessage = Message.Refusal(refusal.russianText, refusal.code)
            null
        } catch (cancelled: CancellationException) {
            // Экран закрыли, не дождавшись ответа. Это не сбой узла:
            // показывать «узел недоступен» на уход с экрана нельзя.
            throw cancelled
        } catch (failure: Exception) {
            nodeAvailable = false
            lastMessage = Message.NodeUnavailable(what, failure::class.simpleName.orEmpty())
            null
        } finally {
            pending--
        }
    }

    /**
     * Идёт ли сейчас обращение к узлу.
     *
     * Кассир жмёт «Пробить чек» и ждёт ответа ОФД секунду-другую. Без
     * признака занятости экран в этот момент неотличим от непринятого
     * нажатия, и чек пробивают второй раз.
     */
    val busy: Boolean get() = pending > 0

    /**
     * Наличные в денежном ящике.
     *
     * Берётся счётчик всей кассы, а не смены: кассир спрашивает, сколько
     * денег в ящике сейчас, а не сколько прошло за смену. Счётчик узел
     * отдаёт в тиынах.
     */
    val cashInDrawer: Long?
        get() = counters.firstOrNull { it.scope == "GLOBAL" && it.key == "cash.sum" }?.value

    fun report(text: String) {
        lastMessage = Message.Done(text)
    }
}

/** Сообщение кассиру. Разделено по смыслу, а не по цвету. */
sealed interface Message {
    data class Done(val text: String) : Message
    data class Refusal(val text: String, val code: String) : Message
    data class NodeUnavailable(val what: String, val reason: String) : Message
}

/** Код отказа узла, означающий закрытую смену. */
private const val SHIFT_NOT_OPEN = "SHIFT_NOT_OPEN"

/** Роль администратора, как её называет узел. */
private const val ADMIN_ROLE = "ADMIN"
