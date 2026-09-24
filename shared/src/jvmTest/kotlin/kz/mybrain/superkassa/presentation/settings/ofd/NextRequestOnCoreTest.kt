package kz.mybrain.superkassa.presentation.settings.ofd

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdAuthInfoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.presentation.settings.SettingsBench
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Данные авторизации кассы в БФД на настоящем ядре: номер следующего
 * запроса виден администратору, а токен кассы не попадает ни на экран,
 * ни в журнал.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NextRequestOnCoreTest {
    private lateinit var desk: SettingsBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        desk = SettingsBench()
    }

    @AfterTest
    fun close() {
        desk.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `администратор видит номер следующего запроса, который считает касса`() {
        desk.enter()
        val model = ofdSettingsModel(desk.app)
        assertTrue(model.state.value.admin, "администратору кнопка не показана")

        model.askNextRequest()

        val expected = own().nextReqNum
        assertEquals(expected, assertNotNull(model.state.value.nextRequest, "номер не показан"))
    }

    @Test
    fun `токен кассы не попадает ни в состояние экрана, ни в журнал`() {
        desk.enter()
        val model = ofdSettingsModel(desk.app)

        model.askNextRequest()

        val token = assertNotNull(own().token, "у кассы нет токена — проверять нечего")
        // Карточка кассы от ядра в состоянии лежит своя, как у всех экранов:
        // сверяется то, что положил сюда сам запрос номера.
        val own = model.state.value.copy(kkm = null).toString()
        assertFalse(own.contains(token), "токен попал в состояние экрана: $own")
        assertFalse(desk.journal.lines.any { it.contains(token) }, "токен попал в журнал")
    }

    @Test
    fun `кассиру кнопки нет — номер касса отдаёт только администратору`() {
        desk.enter(SettingsBench.CASHIER_PIN)
        val model = ofdSettingsModel(desk.app)

        assertFalse(model.state.value.admin)
        assertNull(model.state.value.nextRequest)
    }

    /** Что касса отдаёт о своей авторизации — прямо фасадом, мимо приложения. */
    private fun own() = desk.kassa.api.getOfdAuthInfo(desk.kassa.adminPin, OfdAuthInfoRequest(desk.kassa.kkmId))
}
