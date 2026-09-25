package kz.mybrain.superkassa.presentation.cabinet.signing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.designsystem.picker.WideChoiceSegments
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts

/**
 * Чем подписать — сегментами Material 3 над кнопкой входа.
 *
 * Выбор стоит там, где подпись начинается, и только когда выбирать есть
 * из чего: на компьютере способ один — NCALayer, и ряда нет.
 *
 * @param enabled выбор гаснет, пока подпись идёт: сменить способ посреди
 *   подписи значило бы подписать не тем, что владелец видит.
 */
@Composable
internal fun SignMethodChoice(signing: CabinetSigning, eds: EdsTexts, enabled: Boolean) {
    if (!signing.choosable) return
    val method by signing.method.collectAsScreenState()
    WideChoiceSegments(
        options = signing.methods,
        selected = method,
        label = { nameOf(it, eds) },
        enabled = enabled,
        onSelect = signing::choose
    )
}

/** Название способа. */
internal fun nameOf(method: SignMethod, eds: EdsTexts): String = when (method) {
    SignMethod.NcaLayer -> eds.ncaLayer
    SignMethod.EgovMobile -> eds.egov
    SignMethod.KeyFile -> eds.keyFile
}

/** Подсказка у входа: как подписывает выбранный способ. */
internal fun aboutOf(method: SignMethod, texts: CabinetTexts): String = when (method) {
    SignMethod.NcaLayer -> texts.hints.signIn
    SignMethod.EgovMobile -> texts.eds.aboutEgov
    SignMethod.KeyFile -> texts.eds.aboutKeyFile
}

/** Подсказка ожидания: где сейчас идёт подпись. */
internal fun waitOf(method: SignMethod, texts: CabinetTexts): String = when (method) {
    SignMethod.NcaLayer -> texts.hints.signWait
    SignMethod.EgovMobile -> texts.eds.waitEgov
    SignMethod.KeyFile -> texts.eds.waitKeyFile
}
