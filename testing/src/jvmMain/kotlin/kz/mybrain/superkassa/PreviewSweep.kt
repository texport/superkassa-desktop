package kz.mybrain.superkassa

import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.ComposableMethod
import androidx.compose.runtime.reflect.asComposableMethod
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PanePreviews
import kz.mybrain.superkassa.designsystem.preview.ScreenPreviews
import kz.mybrain.superkassa.designsystem.theme.size.PreviewDevices
import java.io.File
import java.lang.reflect.Method

/**
 * Превью модуля, нарисованные так же, как их рисует Android Studio.
 *
 * Превью, которое не рисуется, молчит до тех пор, пока его не откроют:
 * подставное состояние разошлось с экраном, и Android Studio показывает
 * отказ вместо кадра. Проверка находит все функции превью модуля по его
 * скомпилированным классам — по аннотациям [ScreenPreviews],
 * [PanePreviews] и [ElementPreviews], которые видны во время исполнения, —
 * и рисует каждую: экран — на каждом окне [PreviewDevices], панель
 * и элемент — в окне телефона.
 *
 * Своих `@Preview` мимо этих аннотаций модуль не заводит ([strayPreviews]):
 * такое превью проверка не нашла бы.
 */
object PreviewSweep {

    /** Скомпилированные классы основного кода модуля: проверки идут из каталога модуля. */
    private const val CLASSES = "build/classes/kotlin/jvm/main"

    /** Функция превью: имя для отчёта, окна, в которых её рисовать, и сама функция. */
    class Found(val name: String, val windows: List<Pair<Int, Int>>, val method: ComposableMethod)

    /** Все функции превью модуля. */
    fun found(classes: File = File(CLASSES)): List<Found> = classes.walkTopDown()
        .filter { it.extension == "class" }
        .map { it.relativeTo(classes).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.') }
        .flatMap { name -> classOf(name).declaredMethods.asSequence() }
        .mapNotNull(::previewOf)
        .sortedBy { it.name }
        .toList()

    private fun classOf(name: String): Class<*> = Class.forName(name, false, PreviewSweep::class.java.classLoader)

    private fun previewOf(method: Method): Found? {
        val windows = windowsOf(method)
        val composable = method.asComposableMethod()
        if (windows == null || composable == null) return null
        composable.asMethod().isAccessible = true
        return Found("${method.declaringClass.simpleName}.${method.name}", windows, composable)
    }

    /** Окна превью по его аннотации; `null` — это не превью. */
    private fun windowsOf(method: Method): List<Pair<Int, Int>>? = when {
        method.isAnnotationPresent(ScreenPreviews::class.java) -> PreviewDevices.screens
        method.isAnnotationPresent(PanePreviews::class.java) -> PHONE
        method.isAnnotationPresent(ElementPreviews::class.java) -> PHONE
        else -> null
    }

    /** Превью, которые не нарисовались: имя, окно и причина. */
    fun failures(found: List<Found> = found()): List<String> = found.flatMap { preview ->
        preview.windows.mapNotNull { (width, height) ->
            runCatching {
                RenderProbe(width, height) { preview.method.invoke(currentComposer, null) }.use { it.frame() }
            }.exceptionOrNull()?.let { "${preview.name} ${width}x$height: $it" }
        }
    }

    /**
     * `@Preview` в основном коде мимо общих аннотаций: `путь:строка`.
     *
     * Сами аннотации объявлены в дизайн-системе, в пакете превью, —
     * там `@Preview` и живёт.
     */
    fun strayPreviews(code: Map<String, List<String>> = DesignRules.code()): List<String> = code
        .filterKeys { !it.startsWith(OWN) }
        .flatMap { (path, lines) ->
            lines.withIndex().filter { (_, line) -> STRAY.containsMatchIn(line) }.map { (at, _) -> "$path:${at + 1}" }
        }

    private const val OWN = "designsystem/preview/"
    private val STRAY = Regex("""@Preview\b|tooling\.preview\.Preview\b""")
    private val PHONE = listOf(PreviewDevices.PHONE_WIDTH to PreviewDevices.PHONE_HEIGHT)
}
