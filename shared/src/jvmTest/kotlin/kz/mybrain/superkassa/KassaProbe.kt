package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.asCoroutineDispatcher
import kz.mybrain.superkassa.designsystem.adaptive.WindowClassRoot
import kz.mybrain.superkassa.designsystem.strings.ProvideStrings
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.SuperkassaTheme
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.presentation.shell.bar.ShellBar
import kz.mybrain.superkassa.presentation.shell.frame.MessageHost
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import kz.mybrain.superkassa.strings.api.Language
import java.io.File
import java.util.concurrent.Executors

/**
 * Рабочее окно кассы без окна — с замером того, что на нём стоит.
 *
 * Та же сцена, что у [RenderProbe], но проверке нужно не только «изменился
 * ли кадр», а где стоит кнопка и какой ширины название: сцена отдаёт дерево
 * доступности, и элемент находится по надписи, как его находит кассир.
 *
 * Одна сцена обходит все сочетания ступени шрифта и языка одного окна:
 * каждая новая сцена Compose оставляет за собой несколько мегабайт, и сотня
 * сцен на проверку исчерпывала память всего прогона. Сочетание ставится
 * [show] — содержимое собирается заново, с чистым состоянием.
 */
internal class KassaProbe(
    val width: Int,
    val height: Int,
    appearance: Appearance = Appearance.Light,
    look: Look = Look(),
    language: Language = Language.Ru,
    content: @Composable () -> Unit = {}
) : AutoCloseable {

    private var clock = 0L
    private var look by mutableStateOf(look)
    private var language by mutableStateOf(language)
    private var content by mutableStateOf(content)
    private var generation by mutableStateOf(0)

    private val thread = Executors.newSingleThreadExecutor { work ->
        Thread(work, "kassa-probe").apply { isDaemon = true }
    }

    private val scene = onScene {
        ImageComposeScene(width, height, Density(1f), coroutineContext = thread.asCoroutineDispatcher()) {
            SuperkassaTheme(appearance, this.look) {
                ProvideStrings(this.language) { WindowClassRoot { key(generation) { this.content() } } }
            }
        }
    }

    /** Новое содержимое в той же сцене: ступень, язык и экран — с чистого листа. */
    fun show(look: Look, language: Language, content: @Composable () -> Unit) {
        onScene {
            this.look = look
            this.language = language
            this.content = content
            generation += 1
        }
        frame(SETTLE)
    }

    private fun <T> onScene(work: () -> T): T = thread.submit(work).get()

    /** Кадр; несколько кадров подряд доводят отложенное до экрана. */
    fun frame(settle: Int = 1): ByteArray = onScene {
        // Промежуточные кадры только рисуются: сжимать в картинку каждый
        // из сорока — работа и память впустую.
        repeat(settle - 1) {
            clock += FRAME
            scene.render(clock).close()
        }
        clock += FRAME
        scene.render(clock).use { it.encodeToData()?.bytes ?: ByteArray(0) }
    }

    /** Кадр в `/tmp/adaptive-kassa-<имя>.png`. */
    fun save(name: String) {
        File("/tmp/adaptive-kassa-$name.png").writeBytes(frame(SETTLE))
    }

    /** Все узлы дерева доступности со слитыми надписями. */
    fun nodes(): List<SemanticsNode> = onScene {
        scene.semanticsOwners.flatMap { owner -> walk(owner.rootSemanticsNode) }
    }

    /** Узлы без слияния: отдельный текст внутри строки списка или кнопки. */
    fun parts(): List<SemanticsNode> = onScene {
        scene.semanticsOwners.flatMap { owner -> walk(owner.unmergedRootSemanticsNode) }
    }

    /** Первый узел, надпись которого содержит [text]. */
    fun node(text: String): SemanticsNode? = nodes().firstOrNull { text in it.label() }

    /** Первый отдельный текст, содержащий [text]. */
    fun part(text: String): SemanticsNode? = parts().firstOrNull { text in it.label() }

    /**
     * Обрезан ли текст узла: многоточием, краем или числом строк.
     *
     * Раскладку текста узел отдаёт сам — так проверяется то, что видит
     * кассир, а не то, что лежит в строке.
     */
    fun cut(node: SemanticsNode): Boolean = onScene {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        // Высоту не спрашиваем: строка шрифта бывает на точку выше своей
        // ячейки и без всякой обрезки. Обрезка — это ширина, лишние строки
        // и многоточие.
        layouts.any { layout ->
            layout.multiParagraph.didExceedMaxLines || (0 until layout.lineCount).any {
                layout.isLineEllipsized(it) || layout.getLineRight(it) > layout.size.width + 1
            }
        }
    }

    /**
     * Сжат ли текст узла по высоте: строки выше отведённого ему места.
     *
     * Такой текст не обрезан ни многоточием, ни краем — раскладка отдаёт
     * ему полоску в несколько точек, и строка режется посреди себя.
     */
    fun squeezed(node: SemanticsNode): Boolean = onScene {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        layouts.any { it.multiParagraph.height > node.size.height + 1 }
    }

    /** Ширина набранного текста узла: у поля ввода — его строки целиком. */
    fun textWidth(node: SemanticsNode): Float? = onScene {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        layouts.firstOrNull()?.let { layout -> (0 until layout.lineCount).maxOf { layout.getLineRight(it) } }
    }

    /** Нажатие по середине узла с этой надписью. */
    fun click(text: String) {
        val found = requireNotNull(node(text)) { "на экране нет «$text»" }
        val at = found.boundsInRoot.center
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at)
            scene.sendPointerEvent(PointerEventType.Release, at)
        }
        frame(SETTLE)
    }

    /** Колесо мыши. */
    fun wheel(at: Offset, ticks: Float) {
        onScene {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Scroll, at, scrollDelta = Offset(0f, ticks))
        }
        frame(SETTLE)
    }

    override fun close() {
        onScene { scene.close() }
        thread.shutdownNow()
    }

    private fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap { walk(it) }

    companion object {
        const val FRAME = 16_000_000L
        const val SETTLE = 40
    }
}

/** Надпись узла: текст, описание значка и набранное в поле. */
internal fun SemanticsNode.label(): String = listOfNotNull(
    config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
    config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
    config.getOrNull(SemanticsProperties.EditableText)?.text
).joinToString(" ")

/** Виден ли узел целиком: не срезан ни прокруткой, ни краем окна. */
internal fun SemanticsNode.wholeOnScreen(width: Int, height: Int): Boolean {
    val shown = boundsInRoot
    return size.width > 0 && size.height > 0 &&
        shown.width.toInt() == size.width && shown.height.toInt() == size.height &&
        shown.left >= 0 && shown.top >= 0 && shown.right <= width && shown.bottom <= height
}

/**
 * Рабочее окно: шапка, рельс и раздел в пределе рабочего экрана — как
 * в `WorkShell`, но раздел подаётся готовым, с набранным чеком.
 */
@Composable
internal fun KassaWindow(
    desk: KassaDesk,
    section: Section,
    messages: SnackbarHostState? = null,
    content: @Composable () -> Unit
) {
    val shell by desk.parts.shell.state.collectAsState()
    val look by desk.look.state.collectAsState()
    Scaffold(
        topBar = { ShellBar(desk.parts, shell, section, onSignOut = {}) },
        snackbarHost = { messages?.let { MessageHost(it) } }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionRail(Section.entries, section, look.railCollapsed, {}, { Text("1.0.6") }) {}
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.sectionFrame().fillMaxHeight()) { content() }
            }
        }
    }
}

/**
 * Обходит сочетания окна, ступени и языка: одна сцена на окно.
 *
 * @param check готовит сочетание и проверяет его; возвращает найденные
 *   нарушения.
 */
internal fun eachWindow(
    cases: List<KassaExtremes.Case> = KassaExtremes.CASES,
    appearance: Appearance = Appearance.Light,
    check: (KassaProbe, KassaExtremes.Case) -> List<String>
): List<String> = cases.groupBy { it.width to it.height }.flatMap { (size, group) ->
    KassaProbe(size.first, size.second, appearance).use { probe -> group.flatMap { check(probe, it) } }
}
