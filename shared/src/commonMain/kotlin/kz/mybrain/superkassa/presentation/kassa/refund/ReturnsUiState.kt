package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.domain.kassa.model.refund.matches
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kotlin.time.Clock

/**
 * Возврат: чеки дня, годные в основание, и возврат по выбранному.
 *
 * @property cashInDrawer наличные в ящике, в тиынах: о нехватке кассир
 *   узнаёт до выдачи денег.
 * @property documents документы выбранного дня, все, новые первыми.
 * @property dayRead касса отдала день: пустой и непрочитанный — разные вещи.
 * @property refund возврат по выбранному чеку; `null` — чек не выбран.
 * @property working возврат оформляется: кнопка не принимает второго нажатия.
 * @property confirming кассир нажал «Вернуть», и касса спрашивает сумму вслух:
 *   возврат, как и деньги из ящика, не отменяется.
 * @property channels какими видами контакта можно отправить чек возврата покупателю.
 */
data class ReturnsUiState(
    val kkm: KkmResponse? = null,
    val signedIn: Boolean = false,
    val shiftOpen: Boolean = false,
    val cashInDrawer: Long? = null,
    val domainKind: DomainKind = DomainKind.Trading,
    val paymentTypes: List<PaymentTypeResponse> = emptyList(),
    val vatRates: List<VatRateResponse> = emptyList(),
    val kind: ReturnKind = ReturnKind.Sell,
    val day: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val number: String = "",
    val documents: List<FiscalDocumentResponse> = emptyList(),
    val loading: Boolean = true,
    val dayRead: Boolean = false,
    val refund: RefundDraft? = null,
    val working: Boolean = false,
    val confirming: Boolean = false,
    val channels: ContactChannels = ContactChannels()
) {
    val blocked: Boolean get() = kkm?.isBlocked == true

    /** Чеки дня, годные в основание этого возврата и подходящие под набранный номер. */
    val candidates: List<FiscalDocumentResponse> get() = kind.basisIn(documents).filter { it.matches(number) }

    /** Выбранный чек-основание, пока он виден в списке. */
    val basis: FiscalDocumentResponse? get() = refund?.basis?.takeIf { chosen -> candidates.any { it.id == chosen.id } }

    /** Каналы доставки перечитаны: вид контакта, чей канал пропал, становится «не отправлять». */
    fun withChannels(read: ContactChannels): ReturnsUiState =
        copy(channels = read, refund = refund?.let { it.copy(contact = read.fit(it.contact)) })

    /** Другой вид контакта покупателя; вид с ненастроенным каналом не выбирается. */
    fun chooseContact(kind: ContactKind): ReturnsUiState =
        copy(refund = refund?.let { it.copy(contact = channels.choose(it.contact, kind)) })

    /** Вернуть можно: сумма принята, оплаты сходятся и прежний возврат кончился. */
    val canRefund: Boolean get() = !working && signedIn && refund?.ready == true
}
