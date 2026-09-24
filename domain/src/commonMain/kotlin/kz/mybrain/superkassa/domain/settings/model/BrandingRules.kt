package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse

/**
 * Оформление чека кассы в том виде, в каком касса принимает его правку.
 *
 * Касса отдаёт оформление ответом и принимает запросом, а меняет его
 * целиком: правка одного поля обязана нести все прочие как есть, иначе
 * касса сбросила бы их в значения по умолчанию. У кассы без оформления
 * берутся умолчания самой кассы.
 */
val KkmResponse.brandingRequest: ReceiptBrandingRequest
    get() = branding?.request() ?: ReceiptBrandingRequest()

private fun ReceiptBrandingResponse.request() = ReceiptBrandingRequest(
    language = language,
    headerLogoUrl = headerLogoUrl,
    paperWidthMm = paperWidthMm,
    themeColor = themeColor,
    beforeHeaderMsg = beforeHeaderMsg,
    headerMsg = headerMsg,
    afterHeaderMsg = afterHeaderMsg,
    beforeItemsMsg = beforeItemsMsg,
    afterItemsMsg = afterItemsMsg,
    beforeTotalsMsg = beforeTotalsMsg,
    afterTotalsMsg = afterTotalsMsg,
    beforeQrMsg = beforeQrMsg,
    footerMsg = footerMsg,
    useForceDarkTheme = useForceDarkTheme,
    customBackgroundColorHex = customBackgroundColorHex,
    customCardTopBorderColorHex = customCardTopBorderColorHex,
    ofdTicketAds = ofdTicketAds,
    printOfdTicketAds = printOfdTicketAds
)
