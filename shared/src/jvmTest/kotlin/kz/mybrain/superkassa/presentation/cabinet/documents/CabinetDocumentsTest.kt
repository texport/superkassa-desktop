package kz.mybrain.superkassa.presentation.cabinet.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Документы кабинета строками журнала: что показывается и что печатается.
 *
 * Разбор ответов кабинета проверяет [kz.mybrain.superkassa.data.cabinet.documents.CabinetDocumentsReadTest].
 */
class CabinetDocumentsTest {

    @Test
    fun `у смены печатать нечего — её Z-отчёт стоит своей строкой`() {
        val shift = shiftRow(
            CabinetShift(shiftNumber = 3, state = "CLOSED", saleTotal = Decimal.parse("100.00")),
            textsOf(Language.Ru).cabinet
        )

        assertFalse(shift.entry.printable)
    }

    /**
     * Выручка смены — продажи за вычетом возвратов.
     *
     * Прежде и строка журнала, и подпись «Выручка» брали продажи как есть:
     * у смены 12 кассы 260940000021 стояло 3 570 ₸ при возвратах 1 900 ₸,
     * и сами возвраты были перечислены строкой ниже в той же карточке.
     */
    @Test
    fun `выручка смены уменьшена на возвраты`() {
        val shift = CabinetShift(
            shiftNumber = 12,
            state = "CLOSED",
            receiptsCount = 10,
            saleTotal = Decimal.parse("3570.00"),
            returnTotal = Decimal.parse("1900.00"),
            buyTotal = Decimal.parse("400.00"),
            cashBalance = Decimal.parse("6070.00")
        )

        assertEquals(Decimal.parse("1670.00"), shift.total)
        assertEquals(Decimal.parse("1670.00"), shift.totals?.revenue)
        // Продажи и покупка остаются собой: их владелец сверяет с лентой.
        assertEquals(Decimal.parse("3570.00"), shift.totals?.salesSum)
        assertEquals(Decimal.parse("400.00"), shift.totals?.purchasesSum)
    }

    /**
     * Пустой список за срок не объявляет кассу пустой.
     *
     * Над списком стоят счётчики за всё время. У кассы со ста чеками
     * и выбранной неделей экран говорил «Здесь появится то, что БФД
     * приняла от этой кассы» — рядом с собственной сотней.
     */
    @Test
    fun `пусто за срок и пусто вовсе названы по-разному`() {
        val texts = textsOf(Language.Ru).cabinet
        val hundred = DocumentsOverview(cashRegisterId = "c-1", receiptsCount = 100)
        val week = JournalPeriod.of(JournalSpan.Week)

        assertEquals(
            texts.documentsNoneInPeriod,
            documentsEmpty(DocumentKind.Receipts, week, hundred, texts).title,
            "пустая неделя объявила кассу без чеков"
        )
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Receipts, week, DocumentsOverview(cashRegisterId = "c-1"), texts).title,
            "у кассы без чеков вовсе предложено сменить срок"
        )
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Receipts, JournalPeriod.of(JournalSpan.All), hundred, texts).title,
            "за всё время предложено сменить срок"
        )
        // У смен срока нет вовсе: кабинет их по дате не отдаёт.
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Shifts, week, hundred.copy(shiftsCount = 5), texts).title,
            "смены, которых кабинет по сроку не отдаёт, предложено искать сроком"
        )
    }
}
