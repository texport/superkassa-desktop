package kz.mybrain.superkassa.desktop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.desktop.KassaExtremes.Case
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.ui.DoorShell
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вход при пятидесяти кассах с общим началом названия.
 *
 * Под название кассы была одна строка, и полсотни касс «Касса торгового
 * зала…» различались только многоточием. Меряется, что название выбранной
 * кассы в списке не обрезано, а «Войти» видна целиком и не ниже цели
 * нажатия.
 *
 * Кадры — `/tmp/adaptive-kassa-login-*.png`.
 */
class LoginAdaptiveTest {

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val kkms = KassaExtremes.kkms()
        val session = KassaScene.session("login-${case.tag}", kkm = null, kkms = kkms)
        session.switchLanguage(case.language)
        val texts = stringsOf(case.language)
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            DoorShell(session, remember { CabinetSession() }, remember { SnackbarHostState() })
        }
        probe.frame(KassaProbe.SETTLE)
        val name = requireNotNull(kkms.first().name)
        val row = probe.part(name)
        if (row == null || probe.cut(row)) failures += "${case.tag}: название кассы обрезано"
        probe.click(name)
        probe.save("login-${case.tag}")
        val enter = probe.node(texts.login.enter)
        println("вход ${case.tag}: название ${row?.size}, «Войти» ${enter?.boundsInRoot}")
        if (enter?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: «Войти» не видна"
        if ((enter?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: «Войти» ниже цели нажатия"
        return failures
    }

    @Test
    fun `название кассы различимо, кнопка входа видна`() {
        val failures = eachWindow { probe, case -> check(probe, case) }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val TOUCH = 48
    }
}
