package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.kassa.CoreScene
import java.io.File

/**
 * Оснастка кассовых снимков: касса, какой её видит кассир, и кадр в файл.
 *
 * Экраны кассы спрашивают кассу процесса и держатель входа, поэтому сцене
 * нужна подставная касса ядра и вход, уже сделанный кассиром. Память
 * рабочего места — в памяти проверки: прогон проверок однажды затёр
 * настройки рабочей кассы. Окно с каркасом вокруг раздела собирает
 * модуль каркаса у себя.
 */
object KassaScene {

    /** Касса, какой её видит кассир на рабочем месте. */
    fun kkm(
        state: String = "ACTIVE",
        kgd: String? = "000000200042",
        name: String? = "Касса у входа",
        autonomousSince: Long? = null,
        taxRegime: String? = "GENERAL",
        shiftOpen: Boolean = false
    ): KkmResponse = CoreScene.kkm(state = state, kgd = kgd, name = name).copy(
        autonomousSince = autonomousSince,
        taxRegime = taxRegime,
        isShiftOpen = shiftOpen,
        isProgrammingMode = state == PROGRAMMING
    )

    /**
     * Кадр экрана в файл, чтобы смотреть глазами.
     *
     * Кадров несколько, а сохраняется последний: снекбар отказа, ожидание
     * и появление списка живут в отложенных действиях, и на первом кадре
     * их на экране ещё нет — отказной снимок выходил неотличимым
     * от обычного.
     */
    fun shot(
        name: String,
        width: Int = WIDE,
        height: Int = TALL,
        settle: Int = SETTLE,
        content: @Composable () -> Unit
    ): ByteArray {
        val frame = RenderProbe(width = width, height = height, content = content).use { probe ->
            repeat(settle) { probe.frame() }
            probe.frame()
        }
        File("/tmp/kassa-$name.png").writeBytes(frame)
        return frame
    }

    const val PIN = CoreScene.PIN

    private const val PROGRAMMING = "PROGRAMMING"

    /** Окно кассира на рабочем месте: столько точек даёт каркас разделу. */
    const val WIDE = 1180
    const val TALL = 820

    /** Сколько кадров даётся отложенным действиям, чтобы доехать до экрана. */
    private const val SETTLE = 40
}
