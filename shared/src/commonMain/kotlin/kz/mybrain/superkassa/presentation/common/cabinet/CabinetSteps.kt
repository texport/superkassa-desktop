package kz.mybrain.superkassa.presentation.common.cabinet

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister

/**
 * Кабинет окна глазами мастера подключения: вход, заведение кассы
 * и ожидание подписи — те же, что в разделах кабинета.
 *
 * Мастер ведёт кассу через кабинет, но кабинета не знает: шаги ему
 * отдаёт кабинет окна, а соединяет их каркас. Своя копия каждой формы
 * в мастере однажды разошлась бы с кабинетом.
 */
interface CabinetSteps {

    /** Обращения мастера — работой кабинета окна: его занятость и его слова о помехах. */
    val calls: CabinetCalls

    /** Вошёл ли владелец и занят ли кабинет обращением — читается в разметке. */
    @Composable
    fun session(): CabinetSession

    /** Вход по ЭЦП — та же кнопка, что на двери кабинета: со сроком ожидания и отменой. */
    @Composable
    fun SignIn()

    /**
     * Окно заведения кассы — то же, что в разделе точек.
     *
     * @param factoryNumber заводской номер: известен мастеру, в окно не вводится.
     * @param year год выпуска кассы из того же шага.
     */
    @Composable
    fun AddRegister(factoryNumber: String, year: String, onDismiss: () -> Unit, onAdded: (CabinetRegister) -> Unit)

    /** Ожидание подписи в NCALayer: отсчёт срока и отмена — те же, что на двери входа. */
    @Composable
    fun SignWait(onCancel: () -> Unit)
}

/**
 * Кабинет окна сейчас.
 *
 * @property open владелец вошёл: без входа кабинет не отвечает.
 * @property busy кабинет занят обращением — кнопки ждут вместе с ним.
 */
data class CabinetSession(val open: Boolean = false, val busy: Boolean = false)
