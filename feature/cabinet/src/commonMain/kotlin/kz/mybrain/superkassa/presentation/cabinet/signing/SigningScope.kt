package kz.mybrain.superkassa.presentation.cabinet.signing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Способ подписи окна: подсказки у входа и в ожидании говорят о нём,
 * а не о NCALayer, которого на Android нет.
 */
val LocalSignMethod: ProvidableCompositionLocal<SignMethod> = compositionLocalOf { SignMethod.NcaLayer }

/**
 * Подпись над всем окном: способ — вниз по разметке, просьбы подписывающего —
 * окном Material 3 поверх любого раздела.
 *
 * Подпись просят вход, заявление и мастер подключения, а окно у неё одно:
 * так оно одинаково и в разделе кабинета, и на двери кабинета до входа
 * кассира, и в шаге мастера — и ни один из них о нём не знает.
 *
 * @param cabinet кабинет окна; `null` — кабинета на платформе нет, и подписи тоже.
 */
@Composable
fun SigningScope(cabinet: CabinetViewModel?, content: @Composable () -> Unit) {
    if (cabinet == null) return content()
    val method by cabinet.signature.method.collectAsScreenState()
    val request by cabinet.signature.request.collectAsScreenState()
    CompositionLocalProvider(LocalSignMethod provides method) {
        content()
        request?.let { SignRequestDialog(it, textsOf(LocalLanguage.current).cabinet.eds, cabinet.signature::answer) }
    }
}

/** Окно просьбы: QR eGov mobile или пароль к файлу ключа. */
@Composable
internal fun SignRequestDialog(request: SignRequest, eds: EdsTexts, onAnswer: (SignAnswer) -> Unit) =
    when (request) {
        is SignRequest.EgovMobile -> EgovSignDialog(request, eds) { onAnswer(SignAnswer.Cancel) }
        is SignRequest.KeyPassword -> KeyPasswordDialog(request, eds, onAnswer)
    }
