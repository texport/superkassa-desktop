package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.components.ConfirmActionDialog
import kz.mybrain.superkassa.desktop.ui.components.ConfirmDangerDialog
import kz.mybrain.superkassa.desktop.ui.components.EscapeCloses
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Escape отменяет вопрос перед необратимым.
 *
 * Правило одно на все наложения кассы, а вопрос о закрытии смены его
 * не знал: снятие кассы с учёта Escape отменял, а закрытие смены —
 * нет, и кассир, открывший вопрос по ошибке, искал мышью кнопку
 * «Отмена» там, где везде хватало клавиши.
 */
class ConfirmDialogEscapeTest {

    @Test
    fun `вопрос о смене отменяется клавишей`() {
        var cancelled = false
        RenderProbe(width = SIDE, height = SIDE) {
            ConfirmActionDialog(
                icon = AppIcons.warning,
                what = "Закрыть смену?",
                explain = "Смена закроется Z-отчётом",
                action = "Закрыть",
                cancel = "Отмена",
                onCancel = { cancelled = true },
                onConfirm = {}
            )
        }.use { probe ->
            probe.frame()
            assertTrue(EscapeCloses.press(), "закрывать нечего — вопрос себя не объявил")
        }
        assertTrue(cancelled, "Escape не отменил вопрос")
    }

    @Test
    fun `вопрос о необратимом отменяется клавишей`() {
        var cancelled = false
        RenderProbe(width = SIDE, height = SIDE) {
            ConfirmDangerDialog(
                what = "Убрать кассу с этого рабочего места?",
                explain = "Отменить нельзя",
                action = "Убрать",
                cancel = "Отмена",
                onCancel = { cancelled = true },
                onConfirm = {}
            )
        }.use { probe ->
            probe.frame()
            assertTrue(EscapeCloses.press(), "закрывать нечего — вопрос себя не объявил")
        }
        assertTrue(cancelled, "Escape не отменил вопрос")
    }

    private companion object {
        const val SIDE = 600
    }
}
