package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.strings.common.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Устаревшее сообщение не остаётся на экране.
 *
 * Отказ висит, пока владелец его не закроет, — и в мастере подключения
 * «Кабинет не отвечает по заданному адресу» стояло под четвёртым успешно
 * пройденным шагом: связь давно восстановилась, а строка осталась.
 */
class StaleMessageTest {

    /**
     * Отказ кабинета снимается его же ответом — и только он.
     *
     * Отказ висит, пока владелец его не закроет, а удача снимает его:
     * кабинет ответил, значит прежний отказ уже неправда. Чужое сообщение —
     * отказ кассы — удача кабинета не трогает: его кассиру никто не отменял.
     */
    @Test
    fun `ответ кабинета снимает его прежний отказ, а чужого не трогает`() {
        val app = CoreScene.app(FakeCore())
        val work = CabinetWork(app.talk)

        runBlocking { work.run("probe") { error("кабинет молчит") } }
        assertTrue(app.notices.last is Message.Refusal, "отказ кабинета не показан")
        runBlocking { work.run("probe") { true } }
        assertNull(app.notices.last, "ответ кабинета не снял его прежний отказ")

        val kassa = Message.Refusal("Касса заблокирована", "KKM_BLOCKED")
        app.notices.show(kassa)
        runBlocking { work.run("probe") { true } }
        assertEquals(kassa, app.notices.last, "ответ кабинета снял отказ кассы")
    }

    /**
     * Отказ снимает только удача того же обращения.
     *
     * Прежде любая удача кабинета снимала любой его отказ: чтение карточки
     * кассы падало, следом проходило чтение её состояния — и сообщение
     * о карточке пропадало, не дочитанное владельцем.
     */
    @Test
    fun `удача соседнего обращения не снимает чужой отказ кабинета`() {
        val app = CoreScene.app(FakeCore())
        val work = CabinetWork(app.talk)

        runBlocking { work.run("read register card") { error("кабинет молчит") } }
        val card = app.notices.last
        assertTrue(card is Message.Refusal, "отказ чтения карточки не показан")
        runBlocking { work.run("read register state") { true } }
        assertEquals(card, app.notices.last, "удача чтения состояния сняла отказ чтения карточки")

        runBlocking { work.run("read register card") { true } }
        assertNull(app.notices.last, "удача того же чтения не сняла его отказ")
    }

    /**
     * Язык рабочего места: своего выбора нет — берётся язык системы.
     *
     * Свежее рабочее место встречало владельца казахским независимо
     * от машины. Государственный язык остаётся для системы, языка которой
     * касса не знает.
     */
    @Test
    fun `язык без выбора берётся у системы`() {
        assertEquals(Language.Ru, Language.byCode(code = null, system = "ru"))
        assertEquals(Language.Kk, Language.byCode(code = null, system = "fr"))
        assertEquals(Language.Kk, Language.byCode(code = "kk", system = "ru"))
    }
}
