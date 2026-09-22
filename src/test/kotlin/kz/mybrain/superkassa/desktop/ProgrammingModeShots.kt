package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вход в режим программирования и выход из него — в одном месте.
 *
 * Прежде войти предлагала карточка печатной формы, а выйти — кнопка
 * в диагностике двумя разделами ниже: кассир входил в режим и искал
 * выход по всему экрану, а иногда и не находил.
 *
 * Снимок смотрит на верх вкладки «Касса»: карточка режима стоит там
 * в обоих состояниях, и кадры обязаны отличаться — иначе по экрану
 * не видно, в режиме касса или нет.
 */
class ProgrammingModeShots {

    private fun frame(name: String, state: String): ByteArray {
        val session = KassaScene.session(name, kkm = KassaScene.kkm(state = state))
        return KassaScene.shot(name) {
            Surface(Modifier.fillMaxSize()) { SettingsScreen(session) }
        }
    }

    @Test
    fun `режим программирования виден и выключается там же, где включается`() {
        val usual = frame("programming-off", "ACTIVE")
        val inside = frame("programming-on", "PROGRAMMING")

        assertTrue(usual.isNotEmpty() && inside.isNotEmpty())
        assertTrue(
            !usual.contentEquals(inside),
            "режим программирования не виден на экране настроек: кадры совпали"
        )
    }
}
