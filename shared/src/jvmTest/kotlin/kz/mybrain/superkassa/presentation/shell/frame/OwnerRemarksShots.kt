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
import kz.mybrain.superkassa.OwnerShots
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.presentation.cabinet.places.CabinetPlacesScene
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.Test

/**
 * Кадры по замечаниям владельца в окне каркаса: вход со снекбаром,
 * компания в кабинете, точки кабинета и настройки — в окне ноутбука и на
 * мониторе. Кадры — `/tmp/owner-<экран>-<окно>.png`; карту касс и мастер
 * новой кассы снимают аналитика и мастер у себя тем же порядком.
 */
class OwnerRemarksShots {

    @Test
    fun `вход со снекбаром`() = OwnerShots.each { width, height ->
        val app = CoreScene.app(LoginScene.core(listOf(KassaScene.kkm())))
        app.services.talk.notices.show(Message.Done(CONNECTED))
        OwnerShots.save("door", width, height) { LoginScene.Door(app) }
    }

    @Test
    fun `компания в кабинете`() = OwnerShots.each { width, height ->
        val stage = CabinetStage { path ->
            when {
                path == "/api/company" -> StubReply(COMPANY)
                path.startsWith("/api/reference/okeds/") -> StubReply(OKED)
                else -> StubReply("{}")
            }
        }
        OwnerShots.save("company", width, height) {
            stage.Window {
                Column(Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                    CompanyScreen(stage.cabinet.cabinet, Language.Ru, stage.texts)
                }
            }
        }
    }

    @Test
    fun `точки кабинета`() = OwnerShots.each { width, height ->
        CabinetPlacesScene(width, height).open("owner-places") {}
    }

    @Test
    fun `настройки`() = OwnerShots.each { width, height ->
        val door = KassaScene.desk(kkm = null)
        val admin = KassaScene.desk()
        OwnerShots.save("settings-door", width, height) { Settings(door) }
        OwnerShots.save("settings-admin", width, height) { Settings(admin) }
    }

    @Composable
    private fun Settings(desk: KassaDesk) {
        Surface(Modifier.fillMaxSize()) { SettingsScreen(SettingsScene.board(desk)) }
    }

    private companion object {
        const val CONNECTED = "Касса подключена — можно входить"
        const val COMPANY = """{"id":"c-1","bin":"230140000000","name":"ТОО «Азик и Ко»",""" +
            """"okeds":[{"code":"47111","primary":true}]}"""
        const val OKED = """{"code":"47111","name":"Розничная торговля продуктами","level":"CLASS"}"""
    }
}
