package kz.mybrain.superkassa.presentation.common.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Камера устройства как сканер штрихкодов — для поля штрихкода продажи.
 *
 * Камеру и распознавание даёт платформа: на Android — CameraX и ZXing,
 * на компьютере камеры нет ([available] — `false`), и режима в поле нет.
 * Окно сканера, слова о разрешении и что делать с найденным кодом — общие,
 * у экрана продажи; платформа только спрашивает разрешение и показывает
 * видоискатель.
 */
interface CodeCamera {

    /** Есть ли у машины камера, которой читать коды. */
    val available: Boolean

    /** Разрешила ли система кассе камеру — и как его попросить. */
    @Composable
    fun access(): CameraAccess

    /**
     * Видоискатель: картинка камеры на отведённом месте, распознавание в фоне.
     *
     * @param onCode код прочитан; зовётся один раз на открытие.
     */
    @Composable
    fun Viewfinder(onCode: (String) -> Unit, modifier: Modifier)
}

/** Доступ к камере, как его видит окно сканера. */
sealed interface CameraAccess {

    /** Разрешено: видоискатель можно показывать. */
    data object Granted : CameraAccess

    /** Разрешения нет: [ask] спрашивает его окном системы. */
    class Missing(val ask: () -> Unit) : CameraAccess

    /** В разрешении отказано насовсем: [settings] открывает настройки приложения. */
    class Denied(val settings: () -> Unit) : CameraAccess
}

/** Камера окна для экранов; без платформы — камеры нет. */
val LocalCodeCamera = staticCompositionLocalOf<CodeCamera> { NoCamera }

/** Камеры нет: компьютер, превью и снимки вида. */
private object NoCamera : CodeCamera {
    override val available: Boolean = false

    @Composable
    override fun access(): CameraAccess = CameraAccess.Granted

    @Composable
    override fun Viewfinder(onCode: (String) -> Unit, modifier: Modifier) = Unit
}
