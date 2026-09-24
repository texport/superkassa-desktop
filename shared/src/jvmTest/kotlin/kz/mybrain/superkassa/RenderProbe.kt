package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.asCoroutineDispatcher
import kz.mybrain.superkassa.presentation.common.adaptive.WindowClassRoot
import kz.mybrain.superkassa.presentation.common.strings.ProvideStrings
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.SuperkassaTheme
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.strings.api.Language
import java.awt.Panel
import java.util.concurrent.Executors
import java.awt.event.KeyEvent as AwtKeyEvent

/**
 * Отрисовка экрана без окна.
 *
 * Проверять поведение на длинных списках живьём на кабинете владельца
 * нельзя — там его боевые данные. Сцена рисует тот же состав элементов
 * в картинку: по времени первой отрисовки видно, собирается ли список
 * целиком, а по изменению картинки — доехала ли прокрутка до содержимого.
 *
 * Оформление, надписи и класс окна подставляются те же, что и в окне
 * кассы: без них разметка берёт значения по умолчанию и меряется не то,
 * что видит владелец.
 */
class RenderProbe(
    width: Int = WIDTH,
    height: Int = HEIGHT,
    appearance: Appearance = Appearance.Light,
    look: Look = Look(),
    /** Язык надписей: раздел смотрят на всех трёх, и обрезает подписи не русский. */
    language: Language = Language.Ru,
    content: @Composable () -> Unit
) : AutoCloseable {

    private var clock = 0L

    /**
     * Свой поток на сцену — и состав, и действия, и рисование.
     *
     * Экраны запускают работу в `LaunchedEffect`, и ответ приходит в поток
     * того, кто его завершил: запрос к узлу — в поток ввода-вывода, пауза —
     * в поток таймера. Состояние, записанное оттуда, Compose при следующем
     * кадре видит из другого потока и падает `Detected multithreaded access
     * to SnapshotStateObserver`. Падение плавающее: оно зависит от того,
     * успел ли ответ прийти до кадра, и проверка списка касс валилась
     * через прогон.
     *
     * Своего потока хватает, чтобы всё это шло по одному: сцена получает
     * его же и как контекст своих сопрограмм.
     */
    private val thread = Executors.newSingleThreadExecutor { work ->
        Thread(work, "render-probe").apply { isDaemon = true }
    }

    private val scene = onScene {
        ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
            coroutineContext = thread.asCoroutineDispatcher()
        ) {
            SuperkassaTheme(appearance, look) {
                ProvideStrings(language) { WindowClassRoot(content) }
            }
        }
    }

    /** Выполняет работу в потоке сцены и ждёт её: иначе поток разъедется с кадром. */
    private fun <T> onScene(work: () -> T): T = thread.submit(work).get()

    /** Картинка очередного кадра: по ней видно, изменилось ли содержимое. */
    fun frame(): ByteArray = onScene {
        clock += FRAME
        scene.render(clock).encodeToData()?.bytes ?: ByteArray(0)
    }

    /**
     * Нажатие клавиши.
     *
     * За кассой работают с клавиатуры, и проверяется она так же, как
     * мышь: сцена принимает нажатие, а кадр после него показывает, что
     * из этого вышло.
     *
     * Собрать нажатие без внутреннего разрешения Compose нельзя: снаружи
     * `KeyEvent` не строится ничем, а средства сцены из `ui-test` в сборку
     * не входят. Разрешение живёт только здесь, в оснастке проверок,
     * и в приложение не попадает.
     */
    @OptIn(InternalComposeUiApi::class)
    fun key(key: Key, type: KeyEventType = KeyEventType.KeyDown) {
        onScene { scene.sendKeyEvent(KeyEvent(key = key, type = type)) }
        frame()
    }

    /**
     * Набор текста в поле, получившее ввод.
     *
     * Поиск в классификаторе отзывается только на набранное: пустая
     * строка отдаёт начало списка, и состояние «ничего не нашлось» без
     * набора не воспроизводится вовсе. Одного кода клавиши для этого
     * мало: набранный знак настольное поле берёт из события системы
     * и без него нажатие пропускает. Событие собирается ровно такое,
     * какое приходит от окна при наборе.
     */
    @OptIn(InternalComposeUiApi::class)
    fun type(text: String) {
        text.forEach { symbol ->
            val typed = AwtKeyEvent(TYPIST, AwtKeyEvent.KEY_TYPED, 0L, 0, AwtKeyEvent.VK_UNDEFINED, symbol)
            onScene {
                scene.sendKeyEvent(
                    KeyEvent(
                        key = Key.Unknown,
                        type = KeyEventType.Unknown,
                        codePoint = symbol.code,
                        nativeEvent = typed
                    )
                )
            }
            frame()
        }
        repeat(SETTLE) { frame() }
    }

    /**
     * Нажатие мышью.
     *
     * Ярлычок места на карте выбирают именно им, и проверить выбор иначе
     * нельзя: обработчик нажатия живёт в самом ярлычке.
     */
    fun click(at: Offset) {
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at)
            scene.sendPointerEvent(PointerEventType.Release, at)
        }
        repeat(SETTLE) { frame() }
    }

    /**
     * Перетаскивание мышью: нажатие, несколько шагов пути и отпускание.
     *
     * Карту двигают именно так, и путь идёт шагами, а не одним прыжком:
     * распознавание жеста ждёт, пока указатель уйдёт от места нажатия
     * дальше порога, и только потом считает сдвиг.
     */
    fun drag(from: Offset, to: Offset, steps: Int = DRAG_STEPS) {
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, from)
            scene.sendPointerEvent(PointerEventType.Press, from)
            for (step in 1..steps) {
                val share = step.toFloat() / steps
                scene.sendPointerEvent(PointerEventType.Move, from + (to - from) * share)
            }
            scene.sendPointerEvent(PointerEventType.Release, to)
        }
        repeat(SETTLE) { frame() }
    }

    /**
     * Колесо мыши над списком; кадры после него доводят прокрутку до конца хода.
     *
     * @param across колесо вбок — так прокручивается широкая таблица.
     */
    fun wheel(at: Offset, ticks: Float, across: Boolean = false) {
        val delta = if (across) Offset(ticks, 0f) else Offset(0f, ticks)
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Scroll, at, scrollDelta = delta)
        }
        repeat(SETTLE) { frame() }
    }

    /**
     * Изменилась ли картинка после действия.
     *
     * Кадры добираются до предела, а не считаются один раз: прокрутка
     * длинного списка доезжает не за фиксированное число кадров, и
     * сравнение сразу после действия давало то удачу, то неудачу
     * на одном и том же коде.
     *
     * Между кадрами проба ждёт: движение бывает не только от самой
     * сцены, но и от корутины рядом — отсчёт срока подписи просыпается
     * раз в секунду. Тридцать кадров подряд рисуются мгновенно и такого
     * движения не застают вовсе, поэтому под нагрузкой полного прогона
     * проверка отсчёта падала на исправном коде.
     */
    fun changedFrom(before: ByteArray): Boolean =
        (1..LIMIT).any {
            if (it > 1) Thread.sleep(BETWEEN_FRAMES)
            !frame().contentEquals(before)
        }

    /**
     * Все узлы доступности сцены вместе с окнами поверх экрана — целиком,
     * без слияния.
     *
     * По ним меряют то, до чего модификатором снаружи экрана не дотянуться:
     * высоту карты, место кнопки внутри готового экрана и диалога, ширину
     * поля и столбца. Читаются в потоке сцены, как и всё остальное.
     */
    fun semantics(): List<SemanticsNode> = onScene { everyNode() }

    /** То же, но разбор идёт в потоке сцены: узел читают, пока сцена его держит. */
    fun <T> nodes(read: (List<SemanticsNode>) -> T): T = onScene { read(everyNode()) }

    /** Что сейчас стоит на экране: надпись, роль и место каждого узла. */
    fun nodes(): List<ProbeNode> = onScene { everyNode().map(::ProbeNode) }

    private fun everyNode(): List<SemanticsNode> =
        scene.semanticsOwners.flatMap { it.unmergedRootSemanticsNode.descendants() }

    override fun close() {
        onScene { scene.close() }
        thread.shutdownNow()
    }

    private companion object {
        /** Кто прислал набранный знак: сцена рисует без окна, и окна-хозяина у события нет. */
        val TYPIST = Panel()

        const val WIDTH = 1180
        const val HEIGHT = 820
        const val FRAME = 16_000_000L
        const val SETTLE = 12
        const val DRAG_STEPS = 6
        const val LIMIT = 30

        /** Сколько проба ждёт между кадрами, высматривая движение рядом со сценой. */
        const val BETWEEN_FRAMES = 100L
    }
}

/** Узел и все его потомки: список, по которому ищется надпись. */
private fun SemanticsNode.descendants(): List<SemanticsNode> = listOf(this) + children.flatMap { it.descendants() }

/**
 * Узел семантики, снятый в потоке сцены.
 *
 * @property visible видимая часть узла: обрезана областью прокрутки и окном,
 *   у ушедшего за край — пустая.
 * @property at левый верхний угол без обрезки.
 */
class ProbeNode(node: SemanticsNode) {
    val text: String = listOfNotNull(
        node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
        node.config.getOrNull(SemanticsProperties.EditableText)?.text
    ).joinToString(" ")
    val label: String = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
    val role: Role? = node.config.getOrNull(SemanticsProperties.Role)
    val enabled: Boolean = !node.config.contains(SemanticsProperties.Disabled)

    /** Поле ввода: у него своя ширина, а не ширина подписи. */
    val editable: Boolean = node.config.contains(SemanticsProperties.EditableText)

    /** Прокручивается ли узел по вертикали: так находится список. */
    val scrolls: Boolean = node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
    val visible: Rect = node.boundsInRoot
    val at: Offset = node.positionInRoot
    val width: Int = node.size.width
    val height: Int = node.size.height

    /** Виден ли узел целиком, а не краем из-под прокрутки. */
    val whole: Boolean get() = height > 0 && visible.height >= height - 1 && visible.width >= width - 1

    override fun toString(): String = "«$text$label» ${width}x$height @ ${at.x.toInt()},${at.y.toInt()}"
}

/** Сколько миллисекунд занимает собрать и нарисовать экран в первый раз. */
fun renderMillis(content: @Composable () -> Unit): Long {
    val started = System.nanoTime()
    RenderProbe(content = content).use { it.frame() }
    return (System.nanoTime() - started) / 1_000_000
}
