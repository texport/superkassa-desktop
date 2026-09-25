package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.shell.bar.KkmBarActions
import kz.mybrain.superkassa.presentation.shell.frame.ShellFrame
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.Language
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Оснастка кадров журнала, смен, очереди, кассиров и мастера.
 *
 * Данные предельные: тысячи документов, суммы в миллиарды, номера
 * и признаки предельной длины, имена по шестьдесят знаков. Раздел стоит
 * в окне так же, как у кассира: шапка, рельс и место раздела справа.
 */
internal object HistoryStage {

    /** Размеры окна: наименьшее, по умолчанию, мониторы и планшеты. */
    val SIZES = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)

    /** Где стоит рабочее место раздела: левый край и ширина. */
    class Place(var left: Float = 0f, var width: Int = 0)

    /** Условия кадра: язык, ступень шрифта и оформление. */
    data class Mode(
        val language: Language = Language.Ru,
        val scale: TextScale = TextScale.Normal,
        val appearance: Appearance = Appearance.Light
    ) {
        val tag: String get() = "${language.name.lowercase()}-${scale.code}-${appearance.code}"
    }

    /** Документ с предельными номером, признаком и суммой. */
    fun document(no: Long): FiscalDocumentResponse = CoreScene.document(
        id = "doc-$no",
        type = TYPES[(no % TYPES.size).toInt()],
        amount = BILLIONS - no * PRICE_STEP,
        status = STATUSES[(no % STATUSES.size).toInt()]
    ).copy(
        docNo = LONG_NUMBER - no,
        printedDocumentNumber = LONG_NUMBER - no,
        fiscalSign = LONG_SIGN,
        createdAt = System.currentTimeMillis() - no * MINUTE,
        shiftNo = LONG_SHIFT
    )

    fun documents(count: Int): List<FiscalDocumentResponse> = (1L..count).map(::document)

    fun shifts(count: Int): List<ShiftResponse> = (1L..count).map { no ->
        val opened = System.currentTimeMillis() - no * DAY
        val closed = no > 1
        ShiftResponse(
            id = "shift-$no",
            kkmId = "kkm-1",
            shiftNo = LONG_SHIFT - no,
            status = if (closed) ShiftStatus.CLOSED else ShiftStatus.OPEN,
            openedAt = opened,
            closedAt = if (closed) opened + HALF_DAY else null,
            closeDocumentId = if (closed) "z-$no" else null
        )
    }

    fun tasks(count: Int): List<QueueItemResponse> = (1..count).map { no ->
        QueueItemResponse(
            id = "task-$no",
            lane = "OFFLINE",
            type = "TICKET",
            status = if (no % 2 == 0) "FAILED" else "PENDING",
            attempt = no,
            nextAttemptAt = System.currentTimeMillis() + no * MINUTE,
            lastError = LONG_REASON,
            errorRu = LONG_REASON,
            errorKk = LONG_REASON,
            errorEn = LONG_REASON
        )
    }

    fun cashiers(count: Int): List<UserResponse> = (1..count).map { no ->
        val name = "$no " + LONG_NAME.take(NAME_LENGTH - 2)
        UserResponse(userId = "u-$no", name = name, role = if (no == 1) UserRole.ADMIN else UserRole.CASHIER)
    }

    /** Окно кассы с разделом: шапка, рельс и место раздела справа. */
    @Composable
    fun Window(desk: KassaDesk, section: Section, place: Place, content: @Composable () -> Unit) {
        // Подложка окна та же, что у кассы: без неё тёмное оформление
        // рисовалось по белому.
        val shell by desk.parts.shell.state.collectAsState()
        ShellFrame(
            sections = Section.entries,
            current = section,
            onPick = {},
            topBar = {
                AppTopBar(title = shell.kkm?.name.orEmpty(), subtitleKept = shell.cashier) {
                    KkmBarActions(shell, desk.look, onSignOut = {}, onRefresh = {})
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).onGloballyPositioned {
                    place.left = it.positionInRoot().x
                    place.width = it.size.width
                }
            ) { content() }
        }
    }

    /** Кадр в `/tmp/adaptive-history-<имя>.png`. */
    fun shot(name: String, width: Int, height: Int, mode: Mode, content: @Composable () -> Unit): ByteArray {
        val look = Look(textScale = mode.scale)
        val frame = RenderProbe(width, height, mode.appearance, look, mode.language, content).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File("/tmp/adaptive-history-$name.png").writeBytes(frame)
        return frame
    }

    /**
     * Сколько затенённых строк видно на вертикали [x].
     *
     * Строки журнала затенены через одну: видимых строк вдвое больше
     * затенённых полос, с точностью до одной.
     */
    fun stripes(frame: ByteArray, x: Int, tint: Int): Int {
        val image = ImageIO.read(ByteArrayInputStream(frame))
        var bands = 0
        var inside = false
        for (y in 0 until image.height) {
            val hit = image.getRGB(x.coerceIn(0, image.width - 1), y) == tint
            if (hit && !inside) bands++
            inside = hit
        }
        return bands
    }

    private const val SETTLE = 40
    private const val LONG_NUMBER = 9_999_999_999L
    private const val LONG_SIGN = "4294967295"
    private const val LONG_SHIFT = 65_535L
    private const val BILLIONS = 999_999_999_999L
    private const val PRICE_STEP = 123_457L
    private const val MINUTE = 60_000L
    private const val DAY = 86_400_000L
    private const val HALF_DAY = 43_200_000L
    private const val NAME_LENGTH = 60
    private val TYPES = listOf("SALE", "RETURN", "BUY", "CASH_IN", "CASH_OUT", "X_REPORT", "Z_REPORT", "SHIFT_OPEN")
    private val STATUSES = listOf("SENT", "PENDING", "FAILED", "UNKNOWN_CODE", "SENT")
    private const val LONG_NAME =
        "Нұрсұлтанова-Жаханшахова Гүлназира Серікқызы Айтбаева Мұхамеджанқызы"
    private const val LONG_REASON =
        "ОФД не ответил за отведённое время: соединение с сервером приёма данных разорвано на стороне " +
            "оператора, повтор назначен автоматически; при повторном отказе проверьте сеть кассы, " +
            "настройки прокси и срок действия токена, выданного оператором фискальных данных для этой кассы"
}
