package kz.mybrain.superkassa.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetSignIn
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки двери в кабинет — и нормального вида, и всех отказов.
 *
 * Отказ входа владелец читает не на месте содержимого, а всплывающей
 * строкой окна: экран входа остаётся на месте, и по снимку видно сразу
 * оба — и то, куда он нажмёт снова, и то, что ему сказали. Проверок
 * разбора отказов хватает и без снимков (`CabinetMessageTest`), здесь
 * смотрят на вид: помещается ли строка, на каком она языке и понятно ли
 * из неё, кто именно отказал.
 */
class CabinetDoorShots {

    private val was = AppLog.debugMode

    @AfterTest
    fun restore() = AppLog.switchDebugMode(was)

    private fun stage() = CabinetStage { CabinetReply("{}", HttpStatusCode.OK) }

    private fun door(name: String, problem: CabinetProblem?): ByteArray {
        val stage = stage()
        return shot(name, width = DOOR_WIDTH, height = DOOR_HEIGHT) {
            val screen = @androidx.compose.runtime.Composable {
                CabinetSignIn(stage.session, stage.cabinet, stage.texts)
            }
            if (problem == null) screen() else WithCabinetMessage(problem) { screen() }
        }
    }

    /**
     * Дверь одна при любом режиме.
     *
     * Прежде при включённой отладке на экране входа открывался вход
     * по набранным ИИН и БИН. Его убрали целиком: личность владельца
     * приходит только из сертификата, и режим отладки на дверь больше
     * не влияет.
     */
    @Test
    fun `дверь одна и не зависит от режима отладки`() {
        AppLog.switchDebugMode(false)
        val plain = door("signin-plain", null)
        AppLog.switchDebugMode(true)
        val debug = door("signin-debug", null)

        assertTrue(plain.isNotEmpty() && debug.isNotEmpty())
        assertTrue(plain.contentEquals(debug), "режим отладки всё ещё меняет экран входа")
    }

    /**
     * Ожидание подписи: кнопка занята, пока NCALayer держит окно пароля.
     *
     * Занятость сеанс выставляет сам на время обращения, и подделать её
     * снаружи нечем: сцена снимается, пока обращение не кончилось.
     */
    @Test
    fun `кнопка входа занята, пока ждём подпись`() {
        AppLog.switchDebugMode(false)
        val stage = stage()
        val waiting = CoroutineScope(Dispatchers.Default)
        waiting.launch { stage.cabinet.guard { awaitCancellation() } }
        while (!stage.cabinet.busy) Thread.onSpinWait()
        val frame = shot("signin-signing", width = DOOR_WIDTH, height = DOOR_HEIGHT) {
            CabinetSignIn(stage.session, stage.cabinet, stage.texts)
        }
        waiting.cancel()

        assertTrue(frame.isNotEmpty())
    }

    @Test
    fun `отказные случаи входа показываются владельцу и все они разные`() {
        AppLog.switchDebugMode(false)
        val frames = mapOf(
            "signin-no-ncalayer" to door("signin-no-ncalayer", CabinetProblem.NoNcaLayer),
            "signin-window-closed" to door(
                "signin-window-closed",
                CabinetProblem.SignDeclined(NcaLayer.WINDOW_CLOSED)
            ),
            "signin-certificate-expired" to door(
                "signin-certificate-expired",
                CabinetProblem.SignDeclined("Срок действия сертификата истёк")
            ),
            "signin-401" to door(
                "signin-401",
                CabinetProblem.Refused("SIGNATURE_INVALID", "Подпись не соответствует подписываемым данным")
            ),
            "signin-403" to door(
                "signin-403",
                CabinetProblem.Refused("ACCESS_DENIED", "Company is not allowed to use the cabinet")
            ),
            "signin-unreachable" to door("signin-unreachable", CabinetProblem.Unreachable("ConnectException")),
            "signin-expired" to door("signin-expired", CabinetProblem.SessionExpired)
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "отказы входа неотличимы друг от друга"
        )
    }

    private companion object {
        /** Дверь в кабинет открывается во всё окно рабочего места. */
        const val DOOR_WIDTH = 1180
        const val DOOR_HEIGHT = 700
    }
}
