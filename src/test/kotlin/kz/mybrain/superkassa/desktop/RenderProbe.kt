package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.asCoroutineDispatcher
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.ProvideStrings
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.SuperkassaTheme
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
 * Оформление и надписи подставляются те же, что и в окне кассы: без них
 * разметка берёт значения по умолчанию и меряется не то, что видит
 * владелец.
 */
class RenderProbe(
    width: Int = WIDTH,
    height: Int = HEIGHT,
    appearance: Appearance = Appearance.Light,
    look: Look = Look(),
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
                ProvideStrings(Language.Ru) { content() }
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

    /** Колесо мыши над списком; кадры после него доводят прокрутку до конца хода. */
    fun wheel(at: Offset, ticks: Float) {
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Scroll, at, scrollDelta = Offset(0f, ticks))
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
     */
    fun changedFrom(before: ByteArray): Boolean =
        (1..LIMIT).any { !frame().contentEquals(before) }

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
    }
}

/** Сколько миллисекунд занимает собрать и нарисовать экран в первый раз. */
fun renderMillis(content: @Composable () -> Unit): Long {
    val started = System.nanoTime()
    RenderProbe(content = content).use { it.frame() }
    return (System.nanoTime() - started) / 1_000_000
}
