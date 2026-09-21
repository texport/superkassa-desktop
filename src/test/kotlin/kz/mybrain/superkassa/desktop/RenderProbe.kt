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
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.ProvideStrings
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.SuperkassaTheme

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
    content: @Composable () -> Unit
) : AutoCloseable {

    private var clock = 0L

    private val scene = ImageComposeScene(width = width, height = height, density = Density(1f)) {
        SuperkassaTheme(Appearance.Light) {
            ProvideStrings(Language.Ru) { content() }
        }
    }

    /** Картинка очередного кадра: по ней видно, изменилось ли содержимое. */
    fun frame(): ByteArray {
        clock += FRAME
        return scene.render(clock).encodeToData()?.bytes ?: ByteArray(0)
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
        scene.sendKeyEvent(KeyEvent(key = key, type = type))
        frame()
    }

    /**
     * Нажатие мышью.
     *
     * Ярлычок места на карте выбирают именно им, и проверить выбор иначе
     * нельзя: обработчик нажатия живёт в самом ярлычке.
     */
    fun click(at: Offset) {
        scene.sendPointerEvent(PointerEventType.Move, at)
        scene.sendPointerEvent(PointerEventType.Press, at)
        scene.sendPointerEvent(PointerEventType.Release, at)
        repeat(SETTLE) { frame() }
    }

    /** Колесо мыши над списком; кадры после него доводят прокрутку до конца хода. */
    fun wheel(at: Offset, ticks: Float) {
        scene.sendPointerEvent(PointerEventType.Move, at)
        scene.sendPointerEvent(PointerEventType.Scroll, at, scrollDelta = Offset(0f, ticks))
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

    override fun close() = scene.close()

    private companion object {
        const val WIDTH = 1180
        const val HEIGHT = 820
        const val FRAME = 16_000_000L
        const val SETTLE = 12
        const val LIMIT = 30
    }
}

/** Сколько миллисекунд занимает собрать и нарисовать экран в первый раз. */
fun renderMillis(content: @Composable () -> Unit): Long {
    val started = System.nanoTime()
    RenderProbe(content = content).use { it.frame() }
    return (System.nanoTime() - started) / 1_000_000
}
