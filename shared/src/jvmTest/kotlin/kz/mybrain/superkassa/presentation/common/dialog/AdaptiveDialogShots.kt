package kz.mybrain.superkassa.presentation.common.dialog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.print.preview.component.ReceiptPreview
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.strings.update.updateTexts
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.update.check.UpdateDialog
import kz.mybrain.superkassa.presentation.users.ChangePinDialog
import kz.mybrain.superkassa.presentation.users.PinChange
import kz.mybrain.superkassa.presentation.users.UsersActions
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.Surface as SkiaSurface

/**
 * Диалоги и окно печатной формы на предельных данных: кадры и замеры.
 *
 * Длинное подтверждение по-казахски, название кассы на сотню знаков
 * в заголовке, короткий пин с причиной под полем, длинная причина отказа
 * в окне печатной формы. Кадры — `/tmp/adaptive-settings-dialog-<вид>-<окно>-<язык>-<ступень>.png`;
 * в выводе — ширина окна диалога, его низ против высоты окна, край
 * кнопок и есть ли прокрутка.
 */
class AdaptiveDialogShots {

    private fun shoot(kind: String, width: Int, height: Int, language: Language, scale: TextScale, type: String? = null, content: @Composable () -> Unit) {
        val name = "$kind-${width}x$height-${language.name.lowercase()}-${scale.name.lowercase()}"
        RenderProbe(width, height, look = Look(textScale = scale), language = language) {
            Surface(Modifier.fillMaxSize()) { }
            content()
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            type?.let { probe.type(it) }
            File("/tmp/adaptive-settings-dialog-$name.png").writeBytes(probe.frame())
            val nodes = probe.semantics()
            val surfaces = SettingsMeasure.surfaces(nodes, from = 0, minWidth = 1)
                .filter { it.width < width || it.height < height }
            val dialog = surfaces.maxByOrNull { it.width * it.height }
            val controls = SettingsMeasure.controls(nodes)
            val scrolls = nodes.mapNotNull { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) }
                .count { it.maxValue() > 0f }
            val outside = controls.count { it.right > width || it.bottom > height }
            println(
                "диалог $name: окно ${dialog?.width}×${dialog?.height} слева ${dialog?.left} низ ${dialog?.bottom} из $height; " +
                    "кнопки ${controls.joinToString { "${it.left}..${it.right}@${it.top}" }}; за краем $outside; прокрутка $scrolls"
            )
            assertEquals(0, outside, "$name: кнопка диалога за краем окна")
        }
    }

    private fun everywhere(kind: String, type: String? = null, content: @Composable (Language) -> Unit) {
        SIZES.forEach { (width, height) ->
            LANGUAGES.forEach { language ->
                SCALES.forEach { scale -> shoot(kind, width, height, language, scale, type) { content(language) } }
            }
        }
    }

    @Test
    fun `подтверждение необратимого`() = everywhere("danger") { language ->
        val money = moneyTexts(language)
        ConfirmDangerDialog(
            what = money.kkm.decommissionConfirm.format(SettingsMeasure.LONG_KKM),
            explain = List(REPEAT) { money.kkm.decommissionHint }.joinToString(" "),
            action = money.kkm.decommission,
            cancel = money.drawer.cancel,
            onCancel = {},
            onConfirm = {}
        )
    }

    @Test
    fun `предложение обновления`() = everywhere("update") { language ->
        UpdateDialog(AvailableUpdate(AppVersion(1, 0, 7), "https://example.kz", null), updateTexts(language), {}) {}
    }

    @Test
    fun `смена пина с причиной под полем`() = everywhere("pin") { language ->
        val user = UserResponse(userId = "u-1", name = SettingsMeasure.LONG_KKM, role = UserRole.ADMIN)
        ChangePinDialog(moneyTexts(language), PinChange(user, own = true, pin = "12"), "", object : UsersActions {})
    }

    @Test
    fun `окно заполнения с названием кассы`() = everywhere("form") { _ ->
        val texts = LocalStrings.current
        FormDialog(
            title = texts.preview.drawPin,
            icon = AppIcons.pin,
            action = texts.preview.draw,
            close = texts.preview.close,
            busy = false,
            missing = listOf(texts.common.pin),
            onDismiss = {},
            onAction = {}
        ) {
            Text(SettingsMeasure.LONG_KKM, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = "", onValueChange = {}, label = { Text(texts.common.pin) })
            Text(texts.preview.drawPinHint, modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun `окно печатной формы`() {
        val tape = tapeImage()
        everywhere("preview") { _ -> ReceiptPreview(image = tape, onPrint = {}, onSave = {}) {} }
    }

    @Test
    fun `окно печатной формы с отказом`() = everywhere("preview-refused") { _ ->
        ReceiptPreview(
            image = null,
            trouble = ScreenState.Trouble(LocalStrings.current.preview.missing, LONG_REFUSAL) {},
            onPrint = {},
            onSave = {}
        ) {}
    }

    /** Лента чека: белая полоса со строками, как её рисует узел. */
    private fun tapeImage(): ByteArray {
        val surface = SkiaSurface.makeRasterN32Premul(TAPE_WIDTH, TAPE_HEIGHT)
        surface.canvas.clear(Color.WHITE)
        val ink = Paint().apply { color = Color.BLACK }
        for (line in 0 until TAPE_HEIGHT step TAPE_LINE) {
            surface.canvas.drawRect(
                Rect.makeXYWH(TAPE_MARGIN, line + TAPE_MARGIN, (line * 7 % 400 + 80).toFloat(), 10f),
                ink
            )
        }
        return surface.makeImageSnapshot().encodeToData()!!.bytes
    }

    private companion object {
        val SIZES = listOf(960 to 640, 1180 to 820, 2560 to 1080, 800 to 1280)
        val LANGUAGES = listOf(Language.Ru, Language.Kk)
        val SCALES = listOf(TextScale.Normal, TextScale.Larger)
        const val SETTLE = 20
        const val REPEAT = 4
        const val TAPE_WIDTH = 576
        const val TAPE_HEIGHT = 2400
        const val TAPE_LINE = 28
        const val TAPE_MARGIN = 24f
        const val LONG_REFUSAL = "Узел не нарисовал печатную форму: касса заблокирована ОФД из-за превышения " +
            "срока автономной работы в семьдесят два часа, документы не отправлены, обратитесь к оператору " +
            "фискальных данных и повторите после снятия блокировки через сведения о БФД."
    }
}
