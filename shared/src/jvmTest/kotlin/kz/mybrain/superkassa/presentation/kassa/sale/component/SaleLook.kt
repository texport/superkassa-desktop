package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.LookColors
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.FormActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.measureUnits
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.saleTexts
import kz.mybrain.superkassa.presentation.theme.color.Accent
import kz.mybrain.superkassa.presentation.theme.color.schemeOf
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** Общее для снимков кассовой колонки: надписи, состояние, позиции, размеры кадров. */

/** Надписи, ставки и единицы — те же, что даёт экран продажи. */
@Composable
internal fun Till(state: SaleUiState, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSaleTexts provides saleTexts(Language.Ru),
        LocalVatRates provides state.vat(Language.Ru, LocalStrings.current.enums),
        LocalUnits provides measureUnits(Language.Ru),
        content = content
    )
}

/** Кассир вошёл, смена открыта: чек набирается. */
internal fun open(basket: Basket = Basket(), form: SaleForm = SaleForm()) =
    SaleUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true, basket = basket, form = form)

/** Сколько в кадре точек цвета отказа: им набран любой упрёк экрана. */
internal fun redPixels(png: ByteArray): Int {
    val image = ImageIO.read(ByteArrayInputStream(png))
    val error = schemeOf(Accent.Indigo, dark = false).error
    var found = 0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val pixel = Color(image.getRGB(x, y))
            if (LookColors.distance(pixel, error) < RED_APART) found++
        }
    }
    return found
}

/** Кадр одной кассовой колонки: в него целиком входит форма позиции. */
internal const val ENTRY_WIDE = 460
internal const val ENTRY_TALL = 700

/** Насколько точка должна сойтись с цветом отказа, чтобы счесться красной. */
internal const val RED_APART = 12f

/**
 * Высота кадра набранного чека.
 *
 * Выше обычного кадра: в колонке стоят блок скидок, деньги и кнопка,
 * и на обычной высоте причина под кнопкой уезжала за край кадра —
 * отказные состояния выходили неотличимыми друг от друга.
 */
internal const val RECEIPT_TALL = 1000

/** Высота кадра всей кассовой колонки: три карточки одна под другой. */
internal const val COLUMN_TALL = 1250

/** Кадр на две кассовые колонки рядом: два способа набрать скидку. */
internal const val PAIR_WIDE = 1240

/**
 * Высота кадра, на которую входит форма позиции целиком.
 *
 * Нужна там, где снимок смотрит на сами поля формы: на обычной
 * высоте кассовая колонка обрывается на «Количестве».
 */
internal const val FORM_TALL = 1120

/** Строка на 1 500 ₸ со скидкой, набранной суммой. */
internal val DISCOUNT_TENGE = PositionDraft(
    name = "Хлеб «Тандыр»",
    price = "500.00",
    quantity = "3",
    discount = Adjustment("150.00"),
    measureUnitCode = "796"
)

/** Та же строка со той же скидкой, набранной долей. */
internal val DISCOUNT_PERCENT = DISCOUNT_TENGE.copy(discount = Adjustment("10", AdjustmentUnit.Percent))

/** Ширина кассовой колонки на снимке: та же, что в окне кассира. */
internal val TILL = Sizes.fieldForm + Sizes.fieldPrice + Sizes.fieldQuantity

/** Виды оплаты кассы, где карта объявлена непринимаемой. */
internal val PAYMENTS = listOf(
    PaymentTypeResponse("CASH", TrilingualMessageResponse("Наличные", "Қолма-қол", "Cash"), supported = true),
    PaymentTypeResponse("CARD", TrilingualMessageResponse("Карта", "Карта", "Card"), supported = false),
    PaymentTypeResponse(
        "MOBILE",
        TrilingualMessageResponse("Мобильный платёж", "Мобильді", "Mobile"),
        supported = true
    )
)

/** Действия, которых у снимка нет: нажимать некому. */
internal val NO_FORM = object : FormActions {}
internal val NO_ENTRY = object : EntryActions {}
internal val NO_PAYMENTS = object : PaymentActions {}

/** Позиции для листа чека: дробное количество, акциз и длинное имя. */
internal val POSITIONS = listOf(
    Position(
        name = "Баранина на косточке, охлаждённая",
        price = tenge("3450.00"),
        quantity = decimal("1.450"),
        vatGroup = "VAT_16",
        measureUnitCode = "116"
    ),
    Position(
        name = "Вода питьевая негазированная «Тау Самалы» 5 л в упаковке по шесть бутылок",
        price = tenge("690.00"),
        quantity = decimal("2"),
        vatGroup = "VAT_16",
        measureUnitCode = "796"
    ),
    Position(
        name = "Коньяк «Казахстан» 0,5 л",
        price = tenge("4990.00"),
        quantity = decimal("1"),
        vatGroup = "VAT_16",
        measureUnitCode = "796",
        exciseStamps = listOf("AB1234567890")
    )
)
