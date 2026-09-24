package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.mockCabinet
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.map.MapLook
import kz.mybrain.superkassa.presentation.analytics.map.groupsOf
import kz.mybrain.superkassa.presentation.analytics.map.laidOut
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.presentation.cabinet.places.CabinetPlacesScene
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.setup.ConnectKkmScreen
import kz.mybrain.superkassa.presentation.setup.SetupModels
import kz.mybrain.superkassa.presentation.setup.SetupScene
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Кадры по замечаниям владельца: вход со снекбаром, мастер новой кассы,
 * компания в кабинете, карта касс, точки кабинета и настройки — в окне
 * ноутбука и на мониторе. Кадры — `/tmp/owner-<экран>-<окно>.png`.
 */
class OwnerRemarksShots {

    private fun save(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val frame = inlineMain {
            RenderProbe(width, height, content = content).use { probe ->
                repeat(SETTLE) { probe.frame() }
                probe.frame()
            }
        }
        val file = File("/tmp/owner-$name-${width}x$height.png")
        file.writeBytes(frame)
        assertTrue(file.length() > 0, "кадр $name пуст")
    }

    private fun each(shoot: (Int, Int) -> Unit) = SIZES.forEach { (width, height) -> shoot(width, height) }

    @Test
    fun `вход со снекбаром`() = each { width, height ->
        val app = CoreScene.app(LoginScene.core(listOf(KassaScene.kkm())))
        app.notices.show(Message.Done(CONNECTED))
        save("door", width, height) { LoginScene.Door(app) }
    }

    @Test
    fun `мастер новой кассы`() = each { width, height ->
        val scene = inlineMain { SetupScene().started() }
        val models = inlineMain { SetupModels(scene.model(), scene.registration()) }
        val cabinet = mockCabinet(SetupScene.NO_PLACES)
        save("setup", width, height) { Windowed { ConnectKkmScreen(models, cabinet) {} } }
    }

    @Test
    fun `компания в кабинете`() = each { width, height ->
        val stage = CabinetStage { path ->
            when {
                path == "/api/company" -> StubReply(COMPANY)
                path.startsWith("/api/reference/okeds/") -> StubReply(OKED)
                else -> StubReply("{}")
            }
        }
        save("company", width, height) {
            stage.Window {
                Column(Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                    CompanyScreen(stage.cabinet.cabinet, Language.Ru, stage.texts)
                }
            }
        }
    }

    @Test
    fun `карта касс`() = each { width, height ->
        val view = AnalyticsLook.view((1..MAP_KKMS).map { AnalyticsLook.kkm(it, address = HERE) })
        val start = AnalyticsLook.model()
        val laid = laidOut(view, mapOf(HERE to (AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE)))
        AnalyticsLook.centre(start, laid.placed)
        save("map", width, height) { MapLook(start, laid, groupsOf(laid, start.map.zoom), view) }
    }

    @Test
    fun `точки кабинета`() = each { width, height ->
        CabinetPlacesScene(width, height).open("owner-places") {}
    }

    @Test
    fun `настройки`() = each { width, height ->
        val door = KassaScene.desk(kkm = null)
        val admin = KassaScene.desk()
        save("settings-door", width, height) { Settings(door) }
        save("settings-admin", width, height) { Settings(admin) }
    }

    @Composable
    private fun Settings(desk: KassaDesk) {
        Surface(Modifier.fillMaxSize()) { SettingsScreen(SettingsScene.board(desk)) }
    }

    private companion object {
        val SIZES = listOf(1280 to 800, 1920 to 1080)
        const val SETTLE = 30
        const val MAP_KKMS = 5
        const val HERE = "Алматы, пр. Абая, 1"
        const val CONNECTED = "Касса подключена — можно входить"
        const val COMPANY = """{"id":"c-1","bin":"230140000000","name":"ТОО «Азик и Ко»",""" +
            """"okeds":[{"code":"47111","primary":true}]}"""
        const val OKED = """{"code":"47111","name":"Розничная торговля продуктами","level":"CLASS"}"""
    }
}
