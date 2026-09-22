package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.settings.NodeAddressCard
import kz.mybrain.superkassa.desktop.ui.settings.ServiceAddress
import kz.mybrain.superkassa.desktop.ui.settings.SettingsDrafts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Негодный адрес службы не уходит в настройки.
 *
 * Поле принимало любую непустую строку. Набранный без схемы
 * «192.168.50.35:17700» приложение не разбирает вовсе, а кассиру это
 * выходило как «узел недоступен»: владелец шёл искать сеть и сервер,
 * тогда как искать нужно было опечатку в поле рядом.
 */
class SettingsAddressTest {

    @AfterTest
    fun forget() = SettingsDrafts.forget(SettingsDrafts.Field.NODE_ADDRESS)

    @Test
    fun `адрес без схемы и с пробелами внутри не годится`() {
        listOf(
            "http://127.0.0.1:8080",
            "https://bfd-cabinet.ecc.kz",
            "http://192.168.50.35:17700/"
        ).forEach { assertTrue(ServiceAddress.valid(it), "годный адрес отвергнут: $it") }

        listOf("", "   ", "192.168.50.35:17700", "localhost:8080", "не адрес", "http://", "http:// 1.2.3.4")
            .forEach { assertFalse(ServiceAddress.valid(it), "негодный адрес принят: $it") }
    }

    /** Косая черта на конце и пробелы по краям снимаются, остальное — как набрано. */
    @Test
    fun `адрес сохраняется без краёв и без черты на конце`() {
        assertEquals("http://192.168.50.35:17700", ServiceAddress.tidy("  http://192.168.50.35:17700/  "))
    }

    /**
     * Нажатие «Сохранить» над негодным адресом ничего не меняет.
     *
     * Проверяется нажатием, а не правилом: правило можно завести и не
     * подключить к кнопке, а владелец жмёт именно кнопку.
     */
    @Test
    fun `кнопка над негодным адресом не сохраняет его`() {
        val session = KassaScene.session("settings-bad-address")
        val saved = session.preferences.nodeUrl
        SettingsDrafts.type(SettingsDrafts.Field.NODE_ADDRESS, TYPED)

        RenderProbe(width = WIDTH, height = HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { NodeAddressCard(session) }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/kassa-audit-settings-bad-address.png").writeBytes(probe.frame())
            probe.click(SAVE)
        }

        assertEquals(saved, session.preferences.nodeUrl, "адрес без схемы ушёл в настройки: $TYPED")
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 320
        const val SETTLE = 12

        /** Адрес узла без схемы: приложение не может даже разобрать такой. */
        const val TYPED = "192.168.50.35:17700"

        /** Куда нажать: «Сохранить» стоит справа от поля шириной `Sizes.fieldName`. */
        val SAVE = Offset(400f, 100f)
    }
}
