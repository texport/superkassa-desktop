package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.attemptKey
import kz.mybrain.superkassa.domain.kassa.model.entry.LookupProblem
import kz.mybrain.superkassa.domain.kassa.model.entry.PIECE
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.IssuedReceipt
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleReceipt
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleState
import kz.mybrain.superkassa.domain.kassa.model.sale.blockOf
import kz.mybrain.superkassa.domain.kassa.model.sale.issued
import kz.mybrain.superkassa.presentation.kassa.sale.position.VatRate
import kz.mybrain.superkassa.presentation.kassa.sale.position.vatRatesOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.EnumStrings

/**
 * Продажа: касса, чек и всё набранное поверх него.
 *
 * Состояние живёт в модели окна, а не в экране: кассир уходит в «Деньги»
 * за разменом и возвращается к тому же чеку — с тем же ключом попытки.
 *
 * @property signedIn кассир вошёл и пин принят: без пина чек не пробить.
 * @property shiftOpen смена открыта со слов кассы.
 * @property domainKind отрасль кассы — настройка рабочего места.
 * @property paymentTypes виды оплаты из справочника кассы; пусто — не прочитан.
 * @property vatRates ставки НДС из справочника кассы; пусто — не прочитан.
 * @property draft позиция, набираемая руками.
 * @property search поиск по штрихкоду.
 * @property attemptKey ключ повтора этого чека. Не меняется, пока чек
 *   не принят: если ответ потерялся, повтор с тем же ключом не пробьёт
 *   второй чек.
 * @property issuing чек пробивается: кнопка не принимает второго нажатия.
 * @property collapsed свёрнутые разделы кассовой колонки.
 * @property channels какими видами контакта можно отправить чек покупателю.
 * @property barcodeTurn счёт возвратов фокуса в поле штрихкода: растёт,
 *   когда позиция встала в чек и когда чек пробит, — следующий скан идёт
 *   в штрихкод, а не в «Принято» или в поле цены.
 * @property issued последний пробитый чек: его сумму, сдачу и кнопки показа
 *   и печати видно, пока кассир не начал следующий чек.
 */
data class SaleUiState(
    val kkm: KkmResponse? = null,
    val signedIn: Boolean = false,
    val shiftOpen: Boolean = false,
    val domainKind: DomainKind = DomainKind.Trading,
    val paymentTypes: List<PaymentTypeResponse> = emptyList(),
    val vatRates: List<VatRateResponse> = emptyList(),
    val basket: Basket = Basket(),
    val form: SaleForm = SaleForm(),
    val draft: PositionDraft = PositionDraft(measureUnitCode = PIECE),
    val search: BarcodeSearch = BarcodeSearch(),
    val attemptKey: String = newAttemptKey(),
    val issuing: Boolean = false,
    val collapsed: Set<SalePanel> = emptySet(),
    val channels: ContactChannels = ContactChannels(),
    val issued: IssuedReceipt? = null,
    val barcodeTurn: Int = 0
) {
    /** Фокус — в поле штрихкода: следующий скан пойдёт туда. */
    fun toBarcode(): SaleUiState = copy(barcodeTurn = barcodeTurn + 1)

    /** Чек, как он уйдёт в кассу: корзина, набранное поверх неё и ключ попытки. */
    val receipt: SaleReceipt get() = SaleReceipt(basket, form, domainKind, kkm, vatRates, attemptKey)

    /** Итог чека со скидкой или наценкой на него, в тиынах: его назовут покупателю. */
    val total: Long get() = receipt.total

    /** Снимок для правил: от него зависит, можно ли пробить чек. */
    val rules: SaleState get() = receipt.state(signedIn, shiftOpen, paymentTypes)

    /** Почему чек пробить нельзя; `null` — можно. */
    val block: SaleBlock? get() = blockOf(rules)

    fun expanded(panel: SalePanel): Boolean = panel !in collapsed

    /** Ставки НДС словами кассира и в пределах режима кассы. */
    fun vat(language: Language, texts: EnumStrings): List<VatRate> = vatRatesOf(vatRates, kkm, language, texts)

    /** Плательщик ли касса НДС: только у него есть выбор «на весь чек или по позициям». */
    val vatPayer: Boolean get() = receipt.vatPayer

    /** Ставка кассы по умолчанию — в пределах справочника и режима. */
    val kassaVat: String get() = receipt.kassaVat

    /** НДС этого чека задан на весь чек, а не по позициям. */
    val vatOnReceipt: Boolean get() = receipt.vatOnReceipt

    /** Ставка на весь чек, как она уйдёт в чек; `null` — НДС по позициям. */
    val receiptVat: String? get() = receipt.receiptVat

    /** Ставки у позиции: при НДС на весь чек у позиций ставок нет, и выбирать их незачем. */
    fun positionVat(language: Language, texts: EnumStrings): List<VatRate> =
        if (vatOnReceipt) emptyList() else vat(language, texts)

    /** Итог пробитого чека на экране: только пока корзина следующего пуста. */
    val shownIssued: IssuedReceipt? get() = issued?.takeIf { basket.positions.isEmpty() }

    /**
     * Ручной ввод позиции раскрыт, пока чек пуст.
     *
     * Подсказка пустого чека зовёт ввести позицию вручную, а свёрнутая
     * кассиром в прошлом чеке карточка прятала поля: подсказка лгала.
     * Каждый новый чек начинается с раскрытого ввода; свернуть его можно
     * снова, и выбор помнится как прежде.
     */
    fun withEntryOpen(): SaleUiState =
        if (basket.positions.isEmpty()) copy(collapsed = collapsed - SalePanel.PositionEntry) else this

    /** Принятый чек становится итогом на экране; прочий исход прежний итог не трогает. */
    fun withIssued(receipt: SaleReceipt, answer: Answer<Fiscal>): SaleUiState {
        val fiscal = (answer as? Answer.Done)?.value?.takeUnless { it.rejected } ?: return this
        return copy(issued = receipt.issued(fiscal)).withEntryOpen().toBarcode()
    }

    /** Каналы доставки перечитаны: вид контакта, чей канал пропал, становится «не отправлять». */
    fun withChannels(read: ContactChannels): SaleUiState =
        copy(channels = read, form = form.copy(contact = read.fit(form.contact)))

    /** Другой вид контакта покупателя; вид с ненастроенным каналом не выбирается. */
    fun chooseContact(kind: ContactKind): SaleUiState =
        copy(form = form.copy(contact = channels.choose(form.contact, kind)))

    /** Чек принят: корзина и набранное поверх неё забываются, у следующего чека свой ключ. */
    fun next(): SaleUiState = copy(basket = Basket(), form = form.next(), attemptKey = newAttemptKey())

    /**
     * Чек после ответа кассы: принятый забывается; отвергнутый БФД остаётся
     * для исправления со своим новым ключом; прочий — как был, с прежним.
     */
    fun after(outcome: FiscalOutcome): SaleUiState = when (outcome) {
        FiscalOutcome.Accepted -> next()
        FiscalOutcome.Rejected -> copy(attemptKey = newAttemptKey())
        FiscalOutcome.Unsettled -> this
    }
}

/**
 * Поиск по штрихкоду: что набрано, идёт ли поиск и чем он кончился.
 *
 * @property problem почему поиск не дал позиции; `null` — беды нет.
 * @property asking найденная позиция без цены: цену спрашивают у кассира.
 */
data class BarcodeSearch(
    val barcode: String = "",
    val searching: Boolean = false,
    val problem: LookupProblem? = null,
    val asking: Position? = null
) {
    /** Искать можно: код набран и прежний поиск кончился. */
    val ready: Boolean get() = !searching && barcode.isNotBlank()
}

/**
 * Ключ повтора одной попытки.
 *
 * Не меняется, пока чек не принят: если ответ потерялся в дороге,
 * повтор с тем же ключом не применит фискальный эффект дважды.
 */
fun newAttemptKey(): String = attemptKey()
