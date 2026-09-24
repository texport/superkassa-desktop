package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import kz.mybrain.superkassa.domain.kkm.model.isAutonomous
import kz.mybrain.superkassa.domain.kkm.model.isProgramming

/**
 * Требование кассы к своей настройке.
 *
 * Отделено от надписи намеренно: правило проверяется тестом, а слово
 * выбирается языком кассира на экране.
 */
enum class KkmDemand { Programming, ShiftClosed, QueueEmpty, Online }

/** Требование и то, выполнено ли оно сейчас. */
data class KkmNeed(val demand: KkmDemand, val met: Boolean)

/**
 * При каких условиях касса принимает свою настройку.
 *
 * Касса проверяет то же самое и отвечает отказом, но отказ приходит после
 * нажатия: кассир уже выбрал режим налогообложения, нажал «Сохранить»
 * и только тогда узнал, что сначала нужно закрыть смену. Условия названы
 * до нажатия, а кнопка гаснет ровно по ним.
 *
 * Список требований у каждой настройки свой: печатную форму касса меняет
 * и при открытой смене, а налоги — нет. Общего списка «настройки кассы»
 * здесь нет намеренно: он заставил бы гасить кнопки по самому строгому
 * из требований и запер бы то, что касса принимает.
 */
object KkmSettingRules {

    /**
     * Налоговый режим и ставка по умолчанию.
     *
     * Смена и очередь стоят рядом с режимом программирования потому, что
     * налог считается по настройке на миг пробития: сменить её посреди
     * смены значит свести в один Z-отчёт чеки с разными налогами,
     * а при непустой очереди — отправить в БФД чек, посчитанный
     * по настройке, которой на кассе уже нет.
     */
    fun tax(programming: Boolean, shiftOpen: Boolean, queueWaiting: Boolean): List<KkmNeed> = listOf(
        KkmNeed(KkmDemand.Programming, programming),
        KkmNeed(KkmDemand.ShiftClosed, !shiftOpen),
        KkmNeed(KkmDemand.QueueEmpty, !queueWaiting)
    )

    /**
     * Снятие кассы с учёта.
     *
     * Те же три требования и четвёртое сверх них: снять с учёта кассу,
     * не успевшую отдать документы в БФД, значит потерять их вместе с ней.
     */
    fun decommission(
        programming: Boolean,
        shiftOpen: Boolean,
        queueWaiting: Boolean,
        autonomous: Boolean
    ): List<KkmNeed> = tax(programming, shiftOpen, queueWaiting) + KkmNeed(KkmDemand.Online, !autonomous)

    /**
     * Печатная форма, свои строки чека и автоизъятие.
     *
     * Касса требует только режим программирования: печать чека не меняет
     * ни одной суммы смены, и закрывать её ради ширины ленты незачем.
     */
    fun branding(programming: Boolean): List<KkmNeed> =
        listOf(KkmNeed(KkmDemand.Programming, programming))

    /** Касса примет настройку: выполнено всё, чего она требует. */
    fun met(needs: List<KkmNeed>): Boolean = needs.all { it.met }

    /** Налоги этой кассы — по её состоянию, как его назвала касса. */
    fun tax(kkm: KkmResponse): List<KkmNeed> = tax(kkm.isProgramming, kkm.isShiftOpen, kkm.offlineQueueCount > 0)

    /** Оформление и переключатели этой кассы — по её состоянию. */
    fun branding(kkm: KkmResponse): List<KkmNeed> = branding(kkm.isProgramming)

    /**
     * Сверять с БФД можно при пустой очереди отправки.
     *
     * Условие написано под значком у кнопки, и отказ после нажатия
     * не сообщал бы кассиру ничего нового.
     */
    fun syncable(kkm: KkmResponse): Boolean = kkm.offlineQueueCount == 0

    /** Сведения о кассе касса сверяет только при закрытой смене. */
    fun serviceSyncable(kkm: KkmResponse): Boolean = syncable(kkm) && !kkm.isShiftOpen

    /** Ставку НДС по умолчанию выбирает только плательщик НДС. */
    fun vatChoosable(regime: String?): Boolean = regime != TaxRegime.NO_VAT.name

    /** Снятие этой кассы — по её состоянию, как его назвала касса. */
    fun decommission(kkm: KkmResponse): List<KkmNeed> =
        decommission(kkm.isProgramming, kkm.isShiftOpen, kkm.offlineQueueCount > 0, kkm.isAutonomous)
}
