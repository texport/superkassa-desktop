package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse

/** Чем задан НДС чека. */
enum class VatScope {
    /** Одна ставка на весь чек; у позиций своих ставок нет. */
    Receipt,

    /** Ставка у каждой позиции; новая позиция начинается со ставки кассы. */
    Positions
}

/**
 * НДС чека: на весь чек одной ставкой или по позициям.
 *
 * Способы взаимоисключающие, как скидка на чек и скидка на позицию: касса
 * отвергает чек, где ставка стоит и у чека, и у позиции
 * (`RECEIPT_VAT_SCOPES_CONFLICT`). Поэтому в запрос в каждом способе уходит
 * ставка только одного уровня — такого чека приложение не собирает.
 *
 * Выбор способа есть только у плательщика НДС. Неплательщик налог
 * не выделяет, ставки чека у него быть не может (`RECEIPT_VAT_NOT_ALLOWED`),
 * и его чек всегда «по позициям» — с «Без НДС» у каждой.
 *
 * Выбор живёт при чеке, а не при позиции: переключение не трогает ставок,
 * набранных у позиций, — вернувшись к способу «по позициям», кассир видит
 * их прежними.
 *
 * @property scope выбранный кассиром способ.
 * @property rate ставка на весь чек; `null` — ставка кассы по умолчанию.
 */
data class ReceiptVat(val scope: VatScope = VatScope.Positions, val rate: String? = null) {

    fun switchTo(scope: VatScope): ReceiptVat = copy(scope = scope)

    fun choose(rate: String): ReceiptVat = copy(rate = rate)

    /** Способ, которым НДС уйдёт в чек: у неплательщика — только по позициям. */
    fun scopeAt(payer: Boolean): VatScope = if (payer) scope else VatScope.Positions

    /**
     * Ставка на весь чек, как она уйдёт в запрос.
     *
     * @param kassaRate ставка кассы по умолчанию: ею чек облагается,
     *   пока кассир не выбрал другую.
     * @return `null`, когда НДС по позициям.
     */
    fun receiptRate(payer: Boolean, kassaRate: String): String? =
        if (scopeAt(payer) == VatScope.Receipt) rate ?: kassaRate else null

    /**
     * Ставка позиции, как она уйдёт в запрос.
     *
     * @param own ставка, набранная у позиции.
     * @return `null`, когда НДС на весь чек: у позиции ставки нет.
     */
    fun positionRate(payer: Boolean, own: String): String? =
        if (scopeAt(payer) == VatScope.Positions) own else null
}

/**
 * Плательщик ли касса НДС.
 *
 * Режим не назван — считается плательщиком: у плательщика спрятать ставки
 * значит занизить налог в чеке, а несовпадение касса назовёт отказом.
 */
fun paysVat(kkm: KkmResponse?): Boolean = kkm?.taxRegime != NO_VAT_REGIME

/** Налоговый режим кассы, при котором НДС в чеке не выделяется. */
const val NO_VAT_REGIME = "NO_VAT"
