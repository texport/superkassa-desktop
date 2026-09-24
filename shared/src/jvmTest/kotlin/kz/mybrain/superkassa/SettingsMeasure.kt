package kz.mybrain.superkassa

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage

/**
 * Замеры экрана по узлам смысла: поля, кнопки и поверхности.
 *
 * Кадр показывает, что вышло, а замер — насколько: ширину поля в точках,
 * край кнопки относительно края окна. Меряется `positionInRoot` и размер
 * узла, а не видимая область: у прокручиваемого столбца она обрезана.
 */
internal object SettingsMeasure {

    /** Прямоугольник узла в точках окна. */
    data class Box(val left: Int, val top: Int, val width: Int, val height: Int) {
        val right get() = left + width
        val bottom get() = top + height
        val center get() = Offset(left + width / 2f, top + height / 2f)
    }

    fun SemanticsNode.box(): Box {
        val at = positionInRoot
        return Box(at.x.toInt(), at.y.toInt(), size.width, size.height)
    }

    private fun SemanticsNode.role(): Role? = config.getOrNull(SemanticsProperties.Role)

    private fun SemanticsNode.words(): String =
        config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()

    /** Поля ввода: у них есть редактируемый текст. */
    fun fields(nodes: List<SemanticsNode>): List<Box> =
        nodes.filter { SemanticsProperties.EditableText in it.config }.map { it.box() }

    /** Кнопки и вкладки: всё, что нажимают. */
    fun controls(nodes: List<SemanticsNode>): List<Box> =
        nodes.filter { it.role() in PRESSED }
            .map { it.box() }

    /** Поверхности — карточки и окна: Material отмечает их группой обхода. */
    fun surfaces(nodes: List<SemanticsNode>, from: Int, minWidth: Int = MIN_SURFACE): List<Box> =
        nodes.filter { it.config.getOrNull(SemanticsProperties.IsTraversalGroup) == true }
            .map { it.box() }
            .filter { it.left >= from && it.width >= minWidth }

    /**
     * Ширина столбца содержимого: от левого края раздела до правого края
     * самой правой надписи, поля или кнопки ниже заголовка раздела.
     * Карточка своего узла смысла не имеет, и её ширина — это ширина
     * того, что в ней стоит, плюс поля карточки.
     */
    fun extent(nodes: List<SemanticsNode>, from: Box): Int =
        nodes.filter {
            SemanticsProperties.Text in it.config || SemanticsProperties.EditableText in it.config || it.role() != null
        }
            .map { it.box() }
            .filter { it.left >= from.left && it.top > from.bottom && it.width > 0 }
            .maxOfOrNull { it.right - from.left } ?: 0

    /** Первый узел с этой надписью: по нему нажимают вкладку или кнопку. */
    fun byText(nodes: List<SemanticsNode>, text: String): Box? =
        nodes.firstOrNull { it.words() == text }?.box()

    /** Самый правый узел с этой надписью: заголовок раздела, а не пункт рельса с тем же словом. */
    fun lastByText(nodes: List<SemanticsNode>, text: String): Box? =
        nodes.filter { it.words() == text }.map { it.box() }.maxByOrNull { it.left }

    /** Узел, надпись которого начинается так: для длинных строк. */
    fun byPrefix(nodes: List<SemanticsNode>, prefix: String): Box? =
        nodes.firstOrNull { it.words().startsWith(prefix) }?.box()

    /** Надпись узла, стоящего в этом прямоугольнике: чтобы назвать вылезшее. */
    fun label(nodes: List<SemanticsNode>, at: Box): String =
        nodes.filter { it.words().isNotEmpty() }
            .firstOrNull {
                val b = it.box()
                b.left >= at.left && b.right <= at.right && b.top >= at.top && b.bottom <= at.bottom
            }
            ?.words() ?: "?"

    /** Ширины одним рядом, по возрастанию и без повторов: для строки отчёта. */
    fun widths(boxes: List<Box>): String = boxes.map { it.width }.distinct().sorted().joinToString(",")

    /** Название на сто с лишним знаков: так кассу называют на точке с тремя залами. */
    const val LONG_KKM = "Касса у входа в торговый зал номер два со стороны парковки, " +
        "возле стойки выдачи заказов и кофейного уголка"

    const val LONG_ORG = "Товарищество с ограниченной ответственностью «Алматинская торговая " +
        "компания розничной и оптовой торговли продовольственными товарами»"

    const val LONG_ADDRESS = "Республика Казахстан, город Алматы, Медеуский район, проспект " +
        "Достык, дом 240, нежилое помещение 15, первый этаж"

    /** Строка чека на сто знаков. */
    val LONG_LINE = "Спасибо за покупку! Обмен и возврат товара — в течение 14 дней при наличии чека. ".repeat(2)
        .take(100)

    /** Адрес плиток карты, который не помещается в поле. */
    const val LONG_URL = "https://tiles.maps.example-enterprise-provider.kz/styles/cashier-default/" +
        "{z}/{x}/{y}.png?access_token=placeholder"

    /** Адрес кабинета БФД, который не помещается в поле. */
    const val LONG_CABINET_URL = "https://cabinet.bfd-partner-enterprise-gateway.example.kz/api/v2/" +
        "owners/workplaces/cashier-default"

    /** Адрес поиска адреса на карте, который не помещается в поле. */
    const val LONG_SEARCH_URL = "https://geocoder.maps.example-enterprise-provider.kz/search/v1/" +
        "addresses?country=kz&limit=10"

    /** Касса в режиме программирования: все поля живые, и видно, как они стоят. */
    fun extremeKkm(): KkmResponse = KassaScene.kkm(state = "PROGRAMMING", name = LONG_KKM).let { kkm ->
        kkm.copy(
            ofdServiceInfo = kkm.ofdServiceInfo?.copy(orgTitle = LONG_ORG, orgAddress = LONG_ADDRESS),
            branding = longBranding()
        )
    }

    /** Оформление чека, где каждая строка длиннее поля. */
    private fun longBranding() = ReceiptBrandingResponse(
        language = ReceiptLanguage.MIXED,
        paperWidthMm = 80,
        beforeHeaderMsg = LONG_LINE,
        headerMsg = LONG_LINE,
        afterHeaderMsg = LONG_LINE,
        beforeItemsMsg = LONG_LINE,
        afterItemsMsg = LONG_LINE,
        beforeTotalsMsg = LONG_LINE,
        afterTotalsMsg = LONG_LINE,
        beforeQrMsg = LONG_LINE,
        footerMsg = LONG_LINE,
        printOfdTicketAds = true
    )

    private const val MIN_SURFACE = 200

    /** Роли того, что нажимают: кнопки, вкладки, переключатели, сегменты. */
    private val PRESSED = setOf(Role.Button, Role.Tab, Role.Switch, Role.RadioButton, Role.Checkbox)
}
