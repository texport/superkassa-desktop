package kz.mybrain.superkassa.presentation.cabinet.signin

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.WithCabinetMessage
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.shot
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val texts = textsOf(Language.Ru).cabinet

    /** Дверь до входа: кабинет по своему адресу, никто не вошёл. */
    private val shut = CabinetUiState(address = CabinetSettings.DEFAULT_URL)

    private fun door(name: String, problem: CabinetProblem?, state: CabinetUiState = shut): ByteArray =
        shot(name, width = DOOR_WIDTH, height = DOOR_HEIGHT) {
            val screen = @Composable { CabinetSignIn(state, Language.Ru, texts, object : CabinetActions {}) }
            if (problem == null) screen() else WithCabinetMessage(problem) { screen() }
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
     * Ожидание ответа кабинета: кнопка занята, пока идёт обращение.
     *
     * Занятость — состояние кабинета окна, и снимок собирает его руками:
     * одно начатое обращение, ответа ещё нет.
     */
    @Test
    fun `кнопка входа занята, пока ждём ответа`() {
        AppLog.switchDebugMode(false)
        val frame = door("signin-signing", null, shut.copy(running = 1))

        assertTrue(frame.isNotEmpty())
        assertTrue(!frame.contentEquals(door("signin-idle", null)), "занятая кнопка не отличается от свободной")
    }

    @Test
    fun `отказные случаи входа показываются владельцу и все они разные`() {
        AppLog.switchDebugMode(false)
        val frames = mapOf(
            "signin-no-ncalayer" to door("signin-no-ncalayer", CabinetProblem.NoNcaLayer),
            "signin-window-closed" to door(
                "signin-window-closed",
                CabinetProblem.SignDeclined(Signer.WINDOW_CLOSED)
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
