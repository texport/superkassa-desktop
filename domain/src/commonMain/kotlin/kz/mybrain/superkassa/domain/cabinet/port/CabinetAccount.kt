package kz.mybrain.superkassa.domain.cabinet.port

import kotlinx.coroutines.flow.Flow
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal

/**
 * Вход владельца в личный кабинет.
 *
 * Отдельный от кассового входа: в кабинет входит владелец по своей ЭЦП,
 * а за кассой стоит кассир со своим пином. Доступ, выданный кабинетом,
 * живёт по ту сторону порта: экран знает, кто вошёл, но ключа не видит,
 * и в журнал его отдать ему нечем.
 */
interface CabinetAccount {

    /** Кто вошёл; `null` — никто не входил, вышел или доступ истёк. */
    val owner: Flow<CabinetOwner?>

    /** Адрес кабинета: владелец видит, куда входит. */
    val address: String

    /**
     * Вход по ЭЦП: кабинет выдаёт задачу, владелец её подписывает.
     *
     * Ждать подписи приходится долго — владелец выбирает сертификат и вводит
     * пароль в окне подписывающего, — поэтому вызов длится до его срока.
     *
     * @throws CabinetRefusal кабинет отверг подпись.
     * @throws EdsRefusal подпись не получена.
     */
    suspend fun signIn()

    /** Выход: доступ отзывается и в кабинете, и здесь. */
    suspend fun signOut()
}
