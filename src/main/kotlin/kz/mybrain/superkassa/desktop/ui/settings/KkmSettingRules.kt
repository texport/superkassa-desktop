package kz.mybrain.superkassa.desktop.ui.settings

import kz.mybrain.superkassa.desktop.ui.strings.KkmSetupTexts

/**
 * Требование узла к настройке кассы.
 *
 * Отделено от надписи намеренно: правило проверяется тестом, а слово
 * выбирается языком кассира.
 */
internal enum class KkmDemand(val title: (KkmSetupTexts) -> String) {
    Programming({ it.needProgramming }),
    ShiftClosed({ it.needShiftClosed }),
    QueueEmpty({ it.needQueueEmpty }),
    Online({ it.needOnline })
}

/** Требование и то, выполнено ли оно сейчас. */
internal data class KkmNeed(val demand: KkmDemand, val met: Boolean)

/**
 * При каких условиях узел принимает настройку кассы.
 *
 * Узел проверяет то же самое и отвечает отказом, но отказ приходит после
 * нажатия: кассир уже выбрал режим налогообложения, нажал «Сохранить»
 * и только тогда узнал, что сначала нужно закрыть смену. Условия названы
 * до нажатия, а кнопка гаснет ровно по ним.
 *
 * Список требований у каждой настройки свой: печатную форму узел меняет
 * и при открытой смене, а налоги — нет. Общего списка «настройки кассы»
 * здесь нет намеренно: он заставил бы гасить кнопки по самому строгому
 * из требований и запер бы то, что узел принимает.
 */
internal object KkmSettingRules {

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
     * Узел требует только режим программирования: печать чека не меняет
     * ни одной суммы смены, и закрывать её ради ширины ленты незачем.
     */
    fun branding(programming: Boolean): List<KkmNeed> =
        listOf(KkmNeed(KkmDemand.Programming, programming))

    /** Узел примет настройку: выполнено всё, чего он требует. */
    fun met(needs: List<KkmNeed>): Boolean = needs.all { it.met }
}
